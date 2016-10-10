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
    JedisConfig.setMaxIdle(MainFrameConf.systemProps.getInt("jedisMaxIdle"))
    JedisConfig.setMaxTotal(MainFrameConf.systemProps.getInt("jedisMaxTotal"))
    JedisConfig.setMinEvictableIdleTimeMillis(MainFrameConf.systemProps.getInt("jedisMEM"))
    JedisConfig.setMinIdle(MainFrameConf.systemProps.getInt("jedisMinIdle"))
    JedisConfig.setTestOnBorrow(true)

    println("jedisMaxTotal = " + MainFrameConf.systemProps.getInt("jedisMaxTotal") +
      ", jedisMaxIdle = " + MainFrameConf.systemProps.getInt("jedisMaxIdle") +
      ", jedisMinIdle = " + MainFrameConf.systemProps.getInt("jedisMinIdle") +
      ", jedisMEM = " + MainFrameConf.systemProps.getInt("jedisMEM")
    )

    RoundRobinJedisPool.create().curatorClient(MainFrameConf.systemProps.get("zk"),
                                                MainFrameConf.systemProps.getInt("zkSessionTimeoutMs")
                                              ).zkProxyDir(MainFrameConf.systemProps.get("zkpath"))
                                               .poolConfig(JedisConfig)
                                               .build()
  }
}
