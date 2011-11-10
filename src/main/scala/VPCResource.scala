// JDK refs.
import java.io._
import java.util.logging._
import javax.xml.parsers._
import javax.xml.xpath._
import javax.xml.transform._
import javax.xml.transform.dom._
import javax.xml.transform.stream._
import org.xml.sax._
import org.xml.sax.helpers._

// Scala 2.9 refs.
import scala.xml._
import scala.actors._
import scala.actors.Actor._
import scala.actors.Futures._

// Third party refs.
import org.custommonkey.xmlunit._
import org.restlet._
import org.restlet.data._
import org.restlet.representation._
import org.restlet.resource._
import org.restlet.service._
import org.restlet.ext.xml._
import com.mongodb.casbah.Imports._


/**
 * A Restlet interfacing between MOAB and the Front-End
 * @see ResourceManager for the URI mappings
 * @author Raymond Tay
 * @version 1.0
 */
class VPCResource extends ServerResource {

    /**
     * Gets the configuration of the requested virtual private cloud
     * matched by 'project_id'
     * @param project_id - project's id passed in via the http request
     * @return xml       - a well-formed XML document
     */
	@Get("xml")
	def getVPCconfig() = {
		// extract the data from the database (MongoDB) and return it 
		// as a XML 

		val id = getRequestAttributes().get("project_id")
        val configCol = MongoConnection()("vpc")("configuration")
        val record = MongoDBObject("project_id" -> id)
        val cursor = configCol.find(record)
        if (cursor.hasNext) {
          val rec: DBObject = cursor.next
          val vpcConfig = xml.XML.loadString((rec.get("config_file")).asInstanceOf[String])
          Elem2JDom.toDom(vpcConfig)
        }
	}

    @Post
    def provision(data : Representation) {
        val xmlAsString = new StringWriter
        data.write( xmlAsString )
        val xmlAsElem: Vpc = XmlConverter.fromXML2Vpc( xml.XML.loadString(xmlAsString.toString) )
        val id = getRequestAttributes().get("project_id")
        val configCol = MongoConnection()("vpc")("configuration")
        val record = MongoDBObject("project_id" -> id)

        val cursor = configCol.findOne(record).foreach { rec =>
            // look for changes and process them
            // and return the XML representing the changes. this is not implemented
            // yet so changes are not verified nor done but its a deliverable end Nov'11
            ""
        }
       
        // assuming the previous stuff worked, we wouldn't be at this point
        // so let's move along and actually provision the vpc
        val webTier = mineNProvision( xmlAsElem, "web" ) 
        val appTier = mineNProvision( xmlAsElem, "app" )
        val dbTier =  mineNProvision( xmlAsElem, "db" )

        import scala.xml.Utility._
        val newRecord = MongoDBObject(
          "project_id" -> id,
        "moab_web_rsrv_id" -> webTier,
        "moab_app_rsrv_id" -> appTier,
        "moab_db_rsrv_id" -> dbTier,
        "config_file" -> trim(xmlAsElem.toXML).toString)
        configCol insert newRecord
    }

    private def mineNProvision(root: Vpc, tier: String)  = {
        import scala.collection.mutable._
        val builder = MongoDBList.newBuilder

        tier match {
            case "web" => {
              root.compartmentWeb.machines.foreach { machine => builder += provisionMachine(machine, buildCommandString(machine, root.compartmentWeb.zone)) }
            }
            case "app" => {
              root.compartmentApp.machines.foreach { machine => builder += provisionMachine(machine, buildCommandString(machine, root.compartmentApp.zone)) }
            }
            case "db"  => {
              root.compartmentDB.machines.foreach { machine => builder += provisionMachine(machine, buildCommandString(machine, root.compartmentDB.zone)) }
            }
        }
        builder.result
    }
    
    def provisionMachine(mac: Machine, args: String)  = {
        val f = future { 
        val proc: Process = new ProcessBuilder("./createvm.sh", args ).start
        val input = new Array[Byte](256)
        withBufferedInput(proc.getInputStream)(reader => reader.read(input,0,255)) 
        val xmlString = (new String(input)).trim
        new Status {
                val status = ((xml.XML.loadString(xmlString) \\ "request") \ "status").text
                val vpc  = ((xml.XML.loadString(xmlString) \\ "request") \ "id").text
            } 
        }
        val ret = f()
        MongoDBObject(mac.id -> ret.vpc)
    }
    def withBufferedInput(source: InputStream)(op: InputStream => Unit) {
        val bufferedInput = new BufferedInputStream(source)
        try {
            op(bufferedInput)
        } finally {
            bufferedInput.close()
        }
    }


    private def buildCommandString(mac: Machine, tierZone: String) = {
        import org.antlr.stringtemplate._ 
        import java.text._

        val hostname = new StringTemplate(" -n $hostname$ ")
        val cores    = new StringTemplate(" -c $cores$ ")
        val memory   = new StringTemplate(" -m $memory$ ")
        val bootDisk = new StringTemplate(" -bd $type$,$size$ ")
        val dataDisk = new StringTemplate(" -dd $type$,$size$ ")
        val os       = new StringTemplate(" -o $os$ ")
        val app      = new StringTemplate(" -a $app$ ")
        val zone     = new StringTemplate(" -z $zone$ ")
        val start    = new StringTemplate(" -st $starttime$ ")
        val duration = new StringTemplate(" -d $duration$ ")
        val formatter= new SimpleDateFormat("HH:mm:ss_MM/dd/yyyy")

        val appBuilder = new StringBuilder
        val diskBuilder = new StringBuilder

        def getBootDiskType(disks: Seq[Disk]) : Disk = {
          val d: Seq[Disk] = for( disk <- disks if disk.boot) yield disk
          d(0)
        }
        hostname.setAttribute("hostname", mac.id)
        cores.setAttribute("cores", mac.cores)
        memory.setAttribute("memory", mac.memory)
        os.setAttribute("os", mac.os)
        zone.setAttribute("zone", tierZone)
        start.setAttribute("starttime", formatter.format(new java.util.Date()))
        duration.setAttribute("duration", new java.util.Date())
        bootDisk.setAttribute("type", getBootDiskType(scala.collection.mutable.Seq.empty ++ mac.disks).Type)
        bootDisk.setAttribute("size", getBootDiskType(scala.collection.mutable.Seq.empty ++ mac.disks).size)
        for(disk <- mac.disks if !disk.boot) {
          dataDisk.setAttribute("type", disk.Type)
          dataDisk.setAttribute("size", disk.size)
          diskBuilder.append( dataDisk.toString )
          dataDisk.reset
        }
        for(a <- mac.apps) {
          app.setAttribute("app", a.app)
          appBuilder.append(app.toString)
          app.reset
        }
        hostname.toString + cores.toString + os.toString + zone.toString + start.toString + duration.toString + bootDisk.toString + diskBuilder.toString + appBuilder.toString 
    }
}
