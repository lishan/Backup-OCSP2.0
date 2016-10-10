package com.asiainfo.ocdp.stream.common

import com.asiainfo.ocdp.stream.config.MainFrameConf
import io.codis.jodis.{JedisResourcePool, RoundRobinJedisPool}
import redis.clients.jedis.JedisPoolConfig

/**
 * Created by leo on 8/12/15.
 */
class JodisCacheManager extends RedisCacheManager {

  override def getResource = jedisPool.getResource

  private val jedisPool:JedisResourcePool = {

    val JedisConfig = new JedisPoolConfig()
    JedisConfig.setMaxIdle(MainFrameConf.codisProps.getInt("jedisMaxIdle"))
    JedisConfig.setMaxTotal(MainFrameConf.codisProps.getInt("jedisMaxTotal"))
    JedisConfig.setMinEvictableIdleTimeMillis(MainFrameConf.codisProps.getInt("jedisMEM"))
    JedisConfig.setMinIdle(MainFrameConf.codisProps.getInt("jedisMinIdle"))
    JedisConfig.setTestOnBorrow(true)

    println("jedisMaxTotal = " + MainFrameConf.codisProps.getInt("jedisMaxTotal") +
      ", jedisMaxIdle = " + MainFrameConf.codisProps.getInt("jedisMaxIdle") +
      ", jedisMinIdle = " + MainFrameConf.codisProps.getInt("jedisMinIdle") +
      ", jedisMEM = " + MainFrameConf.codisProps.getInt("jedisMEM")
    )

    RoundRobinJedisPool.create().curatorClient(MainFrameConf.codisProps.get("zk"),
                                                MainFrameConf.codisProps.getInt("zkSessionTimeoutMs")
                                              ).zkProxyDir(MainFrameConf.codisProps.get("zkpath"))
                                               .poolConfig(JedisConfig)
                                               .build()
  }
}
