abstract class Status {
  val vpc:String
  val status: String

  def toXML = 
    <system>
      <vpc>{vpc}</vpc>
      <status>{status}</status>
    </system>
}
