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
        val cursor = configCol.find(record)
        if (cursor.hasNext) {
          val rec: DBObject = cursor.next
          val webIds = xml.XML.loadString((rec.get("moab_web_rsrv_id")).asInstanceOf[String])
          val appIds = xml.XML.loadString((rec.get("moab_app_rsrv_id")).asInstanceOf[String])
          val dbIds = xml.XML.loadString((rec.get("moab_db_rsrv_id")).asInstanceOf[String])
        }
    }
}

