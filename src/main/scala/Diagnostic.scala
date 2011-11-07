// JDK refs.
import java.util._
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
import com.mongodb._
import com.mongodb.casbah._
import com.mongodb.casbah.commons._

/**
 * A Restlet that allows the users to query the state
 * of their provision
 * @see ResourceManager for the URI mappings
 * @author Raymond Tay
 * @version 1.0
 */
class Diagnostic extends ServerResource {

    /**
     * Retrieves the status of the VPC's
     * web, app, db provisioning
     * @param project_id - project's id passed in via http request 
     * @return A XML document
     */
    @Get("xml")
    def getVPCStatus() = {
   		val id = getRequestAttributes().get("project_id")
        val configCol = MongoConnection()("vpc")("configuration")
        val record = MongoDBObject("project_id" -> id)
        configCol.findOne(record).foreach { rec =>
          val webIds = rec.get("moab_web_rsrv_id").asInstanceOf[BasicDBList]
          val appIds = rec.get("moab_app_rsrv_id").asInstanceOf[BasicDBList]
          val dbIds = rec.get("moab_db_rsrv_id").asInstanceOf[BasicDBList]

          import scala.collection.mutable._
          var xmlList = new ListBuffer[Future[Status]]
          val docList = new ListBuffer[Elem]

          webIds.toArray.foreach {
              id => xmlList += checkStatus(id.asInstanceOf[String]) 
          }
          xmlList.foreach {
              x => docList += x().toXML
          }
          xmlList = new ListBuffer[Future[Status]] 
          appIds.toArray.foreach {
              id => xmlList += checkStatus(id.asInstanceOf[String]) 
          }
          xmlList.foreach {
              x => docList += x().toXML
          }
          xmlList = new ListBuffer[Future[Status]] 
          dbIds.toArray.foreach { id => xmlList += checkStatus(id.asInstanceOf[String]) } 
          xmlList.foreach {
              x => docList += x().toXML
          }
      
          buildJDom(docList.toList)
       }
    }

    def checkStatus(projectId: String) : Future[Status] = {
        val f = future { 
        val proc: Process = new ProcessBuilder("./getvmstatus.sh", "-n" + projectId).start
        val input = new Array[Byte](256)
        withBufferedInput(proc.getInputStream)(reader => reader.read(input,0,255)) 
        val xmlString = (new String(input)).trim
        new Status {
                val status = ((xml.XML.loadString(xmlString) \\ "request") \ "status").text
                val vpc  = projectId
            } 
        }
        f
    }
    def withBufferedInput(source: InputStream)(op: InputStream => Unit) {
        val bufferedInput = new BufferedInputStream(source)
        try {
            op(bufferedInput)
        } finally {
            bufferedInput.close()
        }
    }

    def buildJDom(list: scala.List[Elem]) : org.w3c.dom.Document  = {
        def toXML = 
          <system>{ for(ele <- list) yield ele }</system>
        Elem2JDom.toDom(toXML)
    }
}


