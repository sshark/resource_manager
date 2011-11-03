abstract class VpcXml {
    val compartmentWeb: String
    val compartmentApp: String
    val compartmentDB: String
    val machines: Seq[Machine]
    val zone : String
    val firewall : String
    val loadbalancer : String

    def toXML = 
      <system>
        <compartment-web>
          <zone>{zone}</zone>
          <firewall>{firewall}</firewall>
          <load-balancer>{loadbalancer}</load-balancer>
          <machines>
            { for(machine <- machines) yield machine.toXML }
          </machines>
        </compartment-web>
         <compartment-app>
          <zone>{zone}</zone>
          <firewall>{firewall}</firewall>
          <load-balancer>{loadbalancer}</load-balancer>
          <machines>
            { for(machine <- machines) yield machine.toXML }
          </machines>
        </compartment-app>
          <compartment-db>
          <zone>{zone}</zone>
          <firewall>{firewall}</firewall>
          <load-balancer>{loadbalancer}</load-balancer>
          <machines>
            { for(machine <- machines) yield machine.toXML }
          </machines>
        </compartment-db>
      </system>
}
abstract class Machine {
    val id: String
    val Type: String
    val cores: Int
    val memory: Long
    val disks: Seq[Disk]
    val os: String
    val apps: Seq[App]
    val ip_addr : String
    def toXML = 
        <machine>
          <id>{id}</id>
          <type>{Type}</type>
          <cpu-core>{cores}</cpu-core>
          <memory>{memory}</memory>
          <os>{os}</os>
          <ext-ip-address>{ip_addr}</ext-ip-address>
          <disks>{
            for(d <- disks) yield d.toXML
          }
          </disks>
          <applications>{
           for(a <- apps) yield a.toXML
          }
          </applications>
        </machine>
}

abstract class Disk {
    val Type: String
    val size: Int
    val boot: Boolean 
    def toXML = 
    <disk>
      <type>{Type}</type>
      <size>{size}</size>
      <boot>{boot}</boot>
    </disk>
}

abstract class App {
    val app:String
    def toXML = <application>{app}</application>
}

object XmlConverter {
    def fromXML2Machine(node: scala.xml.Node) : Machine = 
      new Machine {
        val id     = ((node \\ "machine") \\ "id").text
        val Type   = ((node \\ "machine") \\ "type").text
        val cores  = ((node \\ "machine") \\ "cpu-core").text.toInt
        val memory = ((node \\ "machine") \\ "memory").text.toLong
        val disks  = for (diskNode <- ((node \\ "machine") \\ "disks")) yield fromXML2Disk(diskNode)
        val os     = ((node \\ "machine") \\ "os").text
        val apps   = for (appNode <- ((node \\ "machine") \\ "applications")) yield fromXML2Application(appNode)
        val ip_addr= ((node \\ "machine") \\ "ext-ip-address").text
    }
    def fromXML2Disk(node: scala.xml.Node):Disk = 
        new Disk {
            val Type = ((node \\ "disk") \\ "type").text
            val size = ((node \\ "disk") \\ "size").text.toInt
            val boot = ((node \\ "disk") \\ "boot").text.toBoolean
        }
    def fromXML2Application(node: scala.xml.Node) : App = 
        new App { val app = (node \\ "application").text }

    def fromXML2Status(node: scala.xml.Node) : Status = 
        new Status {
            val vpc = (node \\ "vpc").text
            val status = (node \\ "status").text
        } 
}

object TestXml extends Application {
    override def main(args: Array[String]) {
        val loadnode = xml.XML.loadFile("/Users/tayboonl/sample.xml")
        println(loadnode.toString)
    }
}


