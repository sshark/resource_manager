import org.restlet._
import org.restlet.data._
import org.restlet.routing._

class ResourceManager extends Application {
	def ResourceManager() {
		setName("ResourceManager")
		setDescription("Interfaces between Adaptive Computing's MOAB and the front-end clients")
		setOwner("HP Labs Singapore")
		setAuthor("Raymond Tay")
	}
	def main(args: Array[String]) {
		val rm = new Server(Protocol.HTTP, 8111)
		rm.setNext(new ResourceManager())
		rm.start()
	}
	override def createInboundRoot():Restlet = {
		val router = new Router(getContext())
        //router.attach("http://localhost:8111/vpc/{project_id}/request/", VPCResource.class)
		router
	}
}
