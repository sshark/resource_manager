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
}
