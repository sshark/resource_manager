abstract class Status {
  val vpc:String
  val status: String

  def toXML = 
    <machine>
      <vpc>{vpc}</vpc>
      <status>{status}</status>
    </machine>
}
