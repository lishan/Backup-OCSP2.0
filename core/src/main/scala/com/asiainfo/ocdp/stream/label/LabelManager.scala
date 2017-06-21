package com.asiainfo.ocdp.stream.label

import java.util.concurrent.ExecutorCompletionService

import com.asiainfo.ocdp.stream.common._
import com.asiainfo.ocdp.stream.constant.LabelConstant
import com.asiainfo.ocdp.stream.tools.{CacheFactory, CacheQryThreadPool, Json4sUtils}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.util.{Failure, Success, Try}

/**
  * The execLabels method will be executed in Executor.
  */
object LabelManager{
  val logger = LoggerFactory.getLogger(this.getClass)

  def execLabels(df: DataFrame): RDD[String] = {

    val broadDiConf = BroadcastManager.getBroadDiConf
    val broadLabels = BroadcastManager.getBroadLabels
    val broadSysProps = BroadcastManager.getBroadSysProps
    val broadCodisProps = BroadcastManager.getBroadCodisProps
    val broadTaskConf = BroadcastManager.getBroadTaskConf

    ComFunc.Func.DFrametoJsonMapPartitions(df)(iter => {
      //      val conf = broadDiConf.value
      val labels = broadLabels.value
      val qryCacheService = new ExecutorCompletionService[List[(String, Array[Byte])]](CacheQryThreadPool.threadPool)
      val hgetAllService = new ExecutorCompletionService[Seq[(String, java.util.Map[String, String])]](CacheQryThreadPool.threadPool)
      // 装载整个批次事件计算中间结果缓存值　label:uk -> 每条信令用map装载
      val busnessKeyList = ArrayBuffer[(String, Map[String, String])]()
      // 装载整个批次打标签操作时，所需要的跟codis数据库交互的key
      val labelQryKeysSet = mutable.Set[String]()
      val cachemap_new = mutable.Map[String, Any]()

      val ukUnion = broadDiConf.value.get("uniqKeys").split(broadDiConf.value.uniqKeysDelim)
      iter.toList.map(jsonStr => {
        val currentLine = Json4sUtils.jsonStr2Map(jsonStr)
        val uk = ukUnion.map(item=>currentLine(item.trim)).mkString(broadDiConf.value.uniqKeyValuesDelim)
        busnessKeyList += (s"${LabelConstant.LABEL_CACHE_PREFIX_NAME}_${broadTaskConf.value.id}:${uk}" -> currentLine)
        // 取出本条数据在打所有标签时所用的查询cache用到的key放入labelQryKeysSet
        labels.foreach(label => {
          val qryRes = Try(label.getQryKeys(currentLine))
          qryRes match {
            case Success(qryKeys) => if (qryKeys != null && qryKeys.nonEmpty) labelQryKeysSet ++= qryKeys
            case Failure(t) => logger.error("Failed to execute datainterface getQryKeys" + label + ".getQryKeys")
          }
        })
      })

      val f1 = System.currentTimeMillis()
      var cachemap_old: Map[String, Any] = null
      val keyList = busnessKeyList.map(line => line._1).toList.distinct
      val batchSize = keyList.size
      println("本批次记录条数：" + batchSize)

      if (broadSysProps.value == null){
        throw new Exception("sys props is not set!")
      }

      if (broadCodisProps.value == null){
        throw new Exception("codis props is not set!")
      }

      //Init Broadcast conf
      BroadcastConf.initProp(broadSysProps.value, broadCodisProps.value)

      try {
        cachemap_old = CacheFactory.getManager.getMultiCacheByKeys(keyList, qryCacheService).toMap
      } catch {
        case ex: Exception =>
          logger.error("= = " * 15 + " got exception in EventSource while get cache")
          throw ex
      }
      val f2 = System.currentTimeMillis()
      println(" 1. 查取一批数据缓存中的交互状态信息 cost time : " + (f2 - f1) + " millis ! ")
      val labelQryData = CacheFactory.getManager.hgetall(labelQryKeysSet.toList, hgetAllService)
      val f3 = System.currentTimeMillis()
      println(" 2. 查取此批数据缓存中的用户相关信息表 cost time : " + (f3 - f2) + " millis ! ")
      // 遍历整个批次的数据，逐条记录打标签

      val jsonList = busnessKeyList.map(enum => {
        // 格式 【"Label:" + uk】
        val key = enum._1
        var value = enum._2
        // 从cache中取出本条记录的中间计算结果值
        var rule_caches = cachemap_old.get(key) match {
          case Some(cache) =>
            cache.asInstanceOf[immutable.Map[String, StreamingCache]]

          case None =>
            //println("rule caches null, key:" + key)
            val cachemap = mutable.Map[String, StreamingCache]()
            labels.foreach(label => cachemap += (label.conf.getId -> null))
            cachemap.toMap
        }
        // 遍历所有所打标签，从cache中取出本条记录对应本标签的中间缓存值，并打标签操作
        labels.foreach(label => {
          // 从cache中取出本条记录所关联的所有标签所用到的用户资料表［静态表］

          val old_cache = rule_caches.get(label.conf.getId) match {
            case Some(cache) =>
              cache
            case None =>
              // println("old cache null, key:" + label.conf.getId)
              null
          }

          // 传入本记录、往期中间记算结果cache、相关的用户资料表，进行打标签操作
          val resAttach = Try(label.attachLabel(value, old_cache, labelQryData))

          resAttach match {
            case Success(resultTuple) => {
              // 增强记录信息，加标签字段
              value = resultTuple._1

              // 更新往期中间记算结果cache
              val newcache = resultTuple._2
              rule_caches = rule_caches.updated(label.conf.getId, newcache)
            }
            case Failure(t) => logger.error(s"Failed to execute attachLabel for ${label}", t)
          }
        })

        // 更新往期中间记算结果cache【"Label:" + uk-> {labelId->rule_caches}】
        cachemap_new += (key -> rule_caches.asInstanceOf[Any])
        cachemap_old += (key -> rule_caches.asInstanceOf[Any])
        Json4sUtils.map2JsonStr(value)
      })

      val f4 = System.currentTimeMillis()
      println(" 3. 遍历一批次数据并打相关联的标签 cost time : " + (f4 - f3) + " millis ! ")
      //update caches to CacheManager
      CacheFactory.getManager.setMultiCache(cachemap_new)
      println(" 4. 更新这批数据的缓存中的交互状态信息 cost time : " + (System.currentTimeMillis() - f4) + " millis ! ")

      jsonList.iterator

    })
  }
}
