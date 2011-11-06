import sbt._

class ResourceManagerProject(info: ProjectInfo) extends DefaultProject(info)
{
  val casbah = "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
}

