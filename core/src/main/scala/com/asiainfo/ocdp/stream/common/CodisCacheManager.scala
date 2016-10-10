package com.asiainfo.ocdp.stream.common

import com.asiainfo.ocdp.stream.config.MainFrameConf
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

/**
 * Created by leo on 8/12/15.
 */
class CodisCacheManager extends RedisCacheManager {

  private val jedisPool: JedisPool = {

    val JedisConfig = new JedisPoolConfig()
	  JedisConfig.setMaxTotal(MainFrameConf.systemProps.getInt("jedisMaxTotal"))
	  JedisConfig.setMaxIdle(MainFrameConf.systemProps.getInt("jedisMaxIdle"))
	  JedisConfig.setMinIdle(MainFrameConf.systemProps.getInt("jedisMinIdle"))
	  JedisConfig.setMinEvictableIdleTimeMillis(MainFrameConf.systemProps.getInt("jedisMEM"))

	  println("jedisMaxTotal = " + MainFrameConf.systemProps.getInt("jedisMaxTotal") +
		  ", jedisMaxIdle = " + MainFrameConf.systemProps.getInt("jedisMaxIdle") +
		  ", jedisMinIdle = " + MainFrameConf.systemProps.getInt("jedisMinIdle") +
		  ", jedisMEM = " + MainFrameConf.systemProps.getInt("jedisMEM")
	  )
    JedisConfig.setTestOnBorrow(true)

    val hp: Tuple2[String, String] = {
      val proxylist = MainFrameConf.systemProps.get("cacheServers").split(",")
       val proxyid  = new java.util.Random().nextInt(proxylist.size)
       val proxyInfo =proxylist(proxyid).split(":")
       val rhost = proxyInfo(0)
       val rip = proxyInfo(1)
      (rhost, rip)
    }
    println("get jedis pool : ip -> " + hp._1 + " ; port -> " + hp._2 +", jedisTimeOut = " + MainFrameConf.systemProps.getInt("jedisTimeOut"))
    new JedisPool(JedisConfig, hp._1, hp._2.toInt, MainFrameConf.systemProps.getInt("jedisTimeOut"))
  }

  override def getResource = jedisPool.getResource

}
