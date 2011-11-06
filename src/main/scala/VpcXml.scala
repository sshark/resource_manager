abstract class CompartmentWeb {
    val zone : String
    val fwrules : Seq[FWRule]
    val loadbalancer : Seq[LoadBalancer]
    val machines : Seq[Machine]

    def toXML = 
    <compartment-web>
        <zone>{zone}</zone>
        <firewall>
            <rules>
                {for(r <- fwrules) yield r.toXML}
            </rules>
        </firewall>
        <load-balancer>
        { for( lb <- loadbalancer) yield lb.toXML } 
        </load-balancer>
        <machines>
        {for(m <- machines) yield m.toXML }
        </machines>
    </compartment-web>
}
abstract class CompartmentApp {
    val zone : String
    val fwrules : Seq[FWRule]
    val machines : Seq[Machine]

    def toXML = 
    <compartment-app>
        <zone>{zone}</zone>
        <firewall>
            <rules>
                {for(r <- fwrules) yield r.toXML}
            </rules>
        </firewall>
        <machines>
        {for(m <- machines) yield m.toXML }
        </machines>
    </compartment-app>
}
abstract class CompartmentDB {
    val zone : String
    val machines : Seq[Machine]

    def toXML = 
    <compartment-db>
        <zone>{zone}</zone>
        <machines>
        {for(m <- machines) yield m.toXML }
        </machines>
    </compartment-db>
}
abstract class Vpc {
    val compartmentWeb : CompartmentWeb
    val compartmentApp : CompartmentApp
    val compartmentDB : CompartmentDB

    def toXML = 
      <system>
        {compartmentWeb.toXML}
        {compartmentApp.toXML}
        {compartmentDB.toXML}
      </system>
}


abstract class FWRule {
    val remote_addr: String
    val direction: String
    val local_addr: String
    val port: Int
    val protocol: String
    def toXML = 
      <rule>
        <remote_addr>{remote_addr}</remote_addr>
        <direction>{direction}</direction>
        <port>{port}</port>
        <protocol>{protocol}</protocol>
      </rule>
}

abstract class LoadBalancer {
    val enabled: Boolean
    val policy : String
    val int_port : Int
    val ext_ip_address : String
    val ext_port : Int
    val includedHosts : Seq[String]

    def toXML = 
      <enabled>{enabled}</enabled>
      <policy>{policy}</policy>
      <int-port>{int_port}</int-port>
      <ext-ip-address>{ext_ip_address}</ext-ip-address>
      <ext-port>{ext_port}</ext-port>
      <included-machines>
        {for (host <- includedHosts) yield "<machine>" + host + "</machine>"}
      </included-machines>
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
    val id : String
    val Type: String
    val size: Int
    val boot: Boolean 
    def toXML = 
    <disk>
      <id>{id}</id>
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
        val id     = ((node \\ "machine") \ "id").text
        val Type   = ((node \\ "machine") \ "type").text
        val cores  = ((node \\ "machine") \\ "cpu-core").text.toInt
        val memory = ((node \\ "machine") \\ "memory").text.toLong
        val disks  = for (diskNode <- (((node \\ "machine") \\ "disks") \\ "disk")) yield fromXML2Disk(diskNode)
        val os     = ((node \\ "machine") \\ "os").text
        val apps   = for (appNode <- (((node \\ "machine") \\ "applications") \\ "application") ) yield fromXML2Application(appNode)
        val ip_addr= ((node \\ "machine") \\ "ext-ip-address").text
    }
    def fromXML2Disk(node: scala.xml.Node):Disk = 
        new Disk {
            val id   = ((node \\ "disk") \\ "id").text
            val Type = ((node \\ "disk") \\ "type").text
            val size     = ((node \\ "disk") \\ "size").text.toInt
            val boot     = ((node \\ "disk") \\ "boot").text.toBoolean
        }
    def fromXML2Application(node: scala.xml.Node) : App = 
        new App { val app = (node \\ "application").text }

    def fromXML2Status(node: scala.xml.Node) : Status = 
        new Status {
            val vpc = (node \\ "vpc").text
            val status = (node \\ "status").text
        } 
    def fromXML2FWRule(node: scala.xml.Node) : FWRule = 
        new FWRule {
            val remote_addr = (node \\ "remote_addr").text
            val direction   = (node \\ "direction").text
            val local_addr  = (node \\ "local_addr").text
            val port        = (node \\ "port").text.toInt
            val protocol    = (node \\ "protocol").text
        }
    def fromXML2LoadBalancer(node: scala.xml.Node) : LoadBalancer =
        new LoadBalancer {
            val enabled        = (node \\ "enabled").text.toBoolean
            val policy         = (node \\ "policy").text
            val int_port       = (node \\ "int-port").text.toInt
            val ext_ip_address = (node \\ "ext-ip-address").text
            val ext_port       = (node \\ "ext-port").text.toInt
            val includedHosts  = 
              for( hostname <- ((node \\ "included-machines") \\ "machine") ) yield hostname.toString
        }
    def fromXML2Vpc(node: scala.xml.Node ) : Vpc = 
      new Vpc {
        val compartmentWeb = 
          new CompartmentWeb {
          val zone = ((node \\ "compartment-web") \ "zone").text
          val fwrules = 
            for (n <- ((((node \\ "compartment-web") \\ "firewall") \\ "rules") \\ "rule") )
              yield fromXML2FWRule(n)
          val loadbalancer = 
            for (n <- ((node \\ "compartment-web") \\ "load-balancer") )
              yield fromXML2LoadBalancer(n)
          val machines = 
            for (n <- (((node \\ "compartment-web") \\ "machines") \\ "machine") )
              yield fromXML2Machine(n)
        }
        val compartmentApp = 
          new CompartmentApp {
          val zone = ((node \\ "compartment-app") \ "zone").text
          val fwrules = 
            for (n <- ((((node \\ "compartment-app") \\ "firewall") \\ "rules") \\ "rule") )
              yield fromXML2FWRule(n)
          val machines = 
            for (n <- (((node \\ "compartment-app") \\ "machines") \\ "machine") )
              yield fromXML2Machine(n)
        }
        val compartmentDB = 
          new CompartmentDB {
          val zone = ((node \\ "compartment-db") \ "zone").text
          val machines = 
            for (n <- (((node \\ "compartment-db") \\ "machines") \\ "machine") )
              yield fromXML2Machine(n)
        }
    }

}

object TestXml extends Application {
    override def main(args: Array[String]) {
        val loadnode = xml.XML.loadFile("/Users/tayboonl/sample.xml")
        println(loadnode.toString)
        val xmlstring = XmlConverter.fromXML2Vpc(loadnode)
        println(xmlstring.toXML)
    }
}


