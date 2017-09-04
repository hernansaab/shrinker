package db.nosql


trait  NoSQL {
  def put(key:String, value:String)

  def get(key:String):Option[String]
}