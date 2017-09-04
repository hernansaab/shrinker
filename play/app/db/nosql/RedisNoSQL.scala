package db.nosql
import play.api.cache.redis.CacheApi
import javax.inject._

class RedisNoSQL @Inject() ( cache: CacheApi ) extends NoSQL {

  def get(key:String):Option[String] = {
    cache.get[ String ](key)
  }

  def put(key:String, value:String): Unit = {
    cache.set(key, value)
  }
}