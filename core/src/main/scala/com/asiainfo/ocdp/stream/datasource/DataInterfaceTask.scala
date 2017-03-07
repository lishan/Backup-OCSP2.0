package com.asiainfo.ocdp.stream.datasource

import java.text.SimpleDateFormat
import java.util.NoSuchElementException
import java.util.concurrent._

import com.asiainfo.ocdp.stream.common.{BroadcastConf, BroadcastManager, EventCycleLife, StreamingCache}
import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, MainFrameConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.{DataSourceConstant, LabelConstant}
import com.asiainfo.ocdp.stream.event.Event
import com.asiainfo.ocdp.stream.manager.StreamTask
import com.asiainfo.ocdp.stream.service.DataInterfaceServer
import com.asiainfo.ocdp.stream.tools._
import org.apache.commons.lang.StringUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.{Accumulator, SparkContext}

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.util.{Failure, Success, Try}

/**
 * Created by surq on 12/09/15
 */
class DataInterfaceTask(taskConf: TaskConf) extends StreamTask {

  val taskDiid = taskConf.getDiid
  val interval = taskConf.getReceive_interval

  val dataInterfaceService = new DataInterfaceServer

  val conf = dataInterfaceService.getDataInterfaceInfoById(taskDiid)
  val labels = dataInterfaceService.getLabelsByIFId(taskDiid)
  val events: Array[Event] = dataInterfaceService.getEventsByIFId(taskDiid)
  conf.setInterval(interval)
  // 原始信令字段个数
  val baseItemSize = conf.getBaseItemsSize


  protected def transform(source: String, schema: StructType, conf: DataInterfaceConf, droppedRecordsCounter: Accumulator[Long], reservedRecordsCounter: Accumulator[Long]): Option[Row] = {
    val delim = conf.get("delim", ",")
    val inputArr = source.split(delim, -1)

    if (inputArr.size != schema.size) {
      droppedRecordsCounter.add(1)
      None
    } else {
      reservedRecordsCounter.add(1)
      Some(Row.fromSeq(inputArr))
    }
  }

  private def conf_check() = {
    /*
     * check the conf in Driver
     * try to get the conf we needed
     * */

    val topic = conf.get(DataSourceConstant.TOPIC_KEY)

    if (StringUtils.isEmpty(topic)) {
      throw new Exception("kafka topic is not set!!!")
    }

    val uniqKeys = conf.get("uniqKeys")

    if (StringUtils.isEmpty(uniqKeys))
      throw new Exception("uniqueKeys is not set!!!")

    val dsconf = conf.getDsConf
    val groupID = {
      try {
        dsconf.get(DataSourceConstant.GROUP_ID_KEY)
      } catch {
        case ex: NoSuchElementException => ""
      }
    }

    if (StringUtils.isEmpty(groupID)) {
      val def_group_name = DataSourceConstant.GROUP_ID_DEF + taskConf.getId + taskConf.getName
      logWarning("no group_id , use default : " + def_group_name)
      dsconf.set(DataSourceConstant.GROUP_ID_KEY, def_group_name)
    }
  }

  final def process(ssc: StreamingContext) = {

    // check the config first
    conf_check()

    val sqlc = new SQLContext(ssc.sparkContext)
    // 用户自定义sql的方法
    registFunction(sqlc)

    //1 根据输入数据接口配置，生成数据流 DStream
    val dataSource = StreamingSourceFactory.createDataSource(ssc, conf)
    val inputStream = dataSource.createStream()

    //1.2 根据输入数据接口配置，生成构造 sparkSQL DataFrame 的 structType
    val schema = conf.getBaseSchema
    // 全量字段: baseItems + udfItems 
    val allItemsSchema = conf.getAllItemsSchema

    //Broad cast configuration from driver to executor
    BroadcastManager.broadcastDiConf(conf)
    BroadcastManager.broadcastLabels(labels)
    BroadcastManager.broadcastSystemProps(MainFrameConf.systemProps)
    BroadcastManager.broadcastCodisProps(MainFrameConf.codisProps)
    BroadcastManager.broadcastTaskConf(taskConf)

    val droppedRecordsCounter = DroppedRecordsCounter.getInstance(ssc.sparkContext)
    val reservedRecordsCounter = ReservedRecordsCounter.getInstance(ssc.sparkContext)

    //2 流数据处理
    inputStream.foreachRDD(rdd => {
      val t0 = System.currentTimeMillis()
      //2.1 流数据转换
      val broadDiConf = BroadcastManager.getBroadDiConf()
      val recover_mode = DataSourceConstant.AT_MOST_ONCE
      val rowRDD = rdd.map(inputArr => {
        val diConf = broadDiConf.value
        transform(inputArr, schema, diConf, droppedRecordsCounter, reservedRecordsCounter)
      }).collect { case Some(row) => row }

      if (rowRDD.partitions.size > 0) {

        val t1 = System.currentTimeMillis()
        println("1.kafka RDD 转换成 rowRDD 耗时 (millis):" + (t1 - t0))
        val dataFrame = sqlc.createDataFrame(rowRDD, schema)
        val t2 = System.currentTimeMillis
        println("2.rowRDD 转换成 DataFrame 耗时 (millis):" + (t2 - t1))
        val filter_expr = conf.get("filter_expr")
        val mixDF = if (filter_expr != null && filter_expr.trim != "") dataFrame.selectExpr(allItemsSchema.fieldNames: _*).filter(filter_expr)
        else dataFrame.selectExpr(allItemsSchema.fieldNames: _*)

        /**
          * update offset BEFORE output the result for AT MOST ONCE
          */
        if (recover_mode == DataSourceConstant.AT_MOST_ONCE)
          StreamingSourceFactory.updateDataSource(broadDiConf.value, rdd)

        if (labels.size > 0) {
          val t3 = System.currentTimeMillis
          println("3.DataFrame 最初过滤不规则数据耗时 (millis):" + (t3 - t2))
          val labelRDD = execLabels(mixDF)
          val t4 = System.currentTimeMillis
          println("4.dataframe 转成rdd打标签耗时(millis):" + (t4 - t3))

          labelRDD.persist()
          // read.json为spark sql 动作类提交job
          val enhancedDF = sqlc.read.json(labelRDD)

          val t5 = System.currentTimeMillis
          println("5.RDD 转换成 DataFrame 耗时(millis):" + (t5 - t4))

          makeEvents(enhancedDF, conf.get("uniqKeys"))

          labelRDD.unpersist()

          println("6.所有业务营销 耗时(millis):" + (System.currentTimeMillis - t5))
        } else {

          val t3 = System.currentTimeMillis
          println("3.DataFrame 最初过滤不规则数据耗时 (millis):" + (t3 - t2))

          mixDF.persist()
          mixDF.count()
          val t4 = System.currentTimeMillis
          println("4.mixDF count耗时(millis):" + (t4 - t3))

          makeEvents(mixDF, conf.get("uniqKeys"))
          mixDF.unpersist()
          println("6.所有业务营销 耗时(millis):" + (System.currentTimeMillis - t4))

        }

        logInfo(s"Dropped ${droppedRecordsCounter.value} records since their schema size is ${schema.size} not matching records field size.")
        logInfo(s"Reserved ${reservedRecordsCounter.value} records successfully.")

        if (MainFrameConf.systemProps.getBoolean(MainFrameConf.MONITOR_RECORDS_CORRECTNESS_ENABLE, false)){
          MonitorUtils.outputRecordsCorrectness(taskConf.id,reservedRecordsCounter.value,droppedRecordsCounter.value,ssc.sparkContext.applicationId)
        }
      }
      else {
        println("当前时间片内正确输入格式的流数据为空, 不做任何处理.")
      }

      /**
        * update offset AFTER output the result for AT LEAST ONCE
        */
      if (recover_mode == DataSourceConstant.AT_LEAST_ONCE)
        StreamingSourceFactory.updateDataSource(broadDiConf.value, rdd)
    })
  }

  /**
   * 自定义slq 方法注册
   */
  def registFunction(sqlc: SQLContext) {
    sqlc.udf.register("Conv", (data: String) => {
      val lc = Integer.toHexString(data.toInt).toUpperCase
      if (lc.length < 4) { val concatStr = "0000" + lc; concatStr.substring(concatStr.length - 4) } else lc
    })

    sqlc.udf.register("concat", (firststr: String, secondstr: String) => firststr + secondstr)
    sqlc.udf.register("from_unixtime", (date: String, format: String) => (new SimpleDateFormat(format)).parse(date).toString())
    sqlc.udf.register("unix_timestamp", () => System.currentTimeMillis().toString)
    sqlc.udf.register("currenttimesub", (subduction: Int) => (System.currentTimeMillis - subduction).toString)
    sqlc.udf.register("currenttimeadd", (add: Int) => (System.currentTimeMillis + add).toString)
  }

  final def readSource(ssc: StreamingContext): DStream[String] = {
    StreamingInputReader.readSource(ssc, conf)
  }

  /**
   * 字段增强：根据uk从codis中取出相关关联数据，进行打标签操作
   */
  def execLabels(df: DataFrame): RDD[String] = {

    val broadDiConf = BroadcastManager.getBroadDiConf
    val broadLabels = BroadcastManager.getBroadLabels
    val broadSysProps = BroadcastManager.getBroadSysProps
    val broadCodisProps = BroadcastManager.getBroadCodisProps
    val broadTaskConf = BroadcastManager.getBroadTaskConf

    df.toJSON.mapPartitions(iter => {
//      val conf = broadDiConf.value
      val labels = broadLabels.value
      val qryCacheService = new ExecutorCompletionService[List[(String, Array[Byte])]](CacheQryThreadPool.threadPool)
      val hgetAllService = new ExecutorCompletionService[Seq[(String, java.util.Map[String, String])]](CacheQryThreadPool.threadPool)
      // 装载整个批次事件计算中间结果缓存值　label:uk -> 每条信令用map装载
      val busnessKeyList = ArrayBuffer[(String, Map[String, String])]()
      // 装载整个批次打标签操作时，所需要的跟codis数据库交互的key
      val labelQryKeysSet = mutable.Set[String]()
      val cachemap_new = mutable.Map[String, Any]()
      val ukUnion = conf.get("uniqKeys").split(":")
      iter.toList.map(jsonStr => {
        val currentLine = Json4sUtils.jsonStr2Map(jsonStr)
        val uk = ukUnion.map(currentLine(_)).mkString(",")
        busnessKeyList += (s"${LabelConstant.LABEL_CACHE_PREFIX_NAME}_${broadTaskConf.value.name}:${uk}" -> currentLine)
        // 取出本条数据在打所有标签时所用的查询cache用到的key放入labelQryKeysSet
        labels.foreach(label => {
          val qryRes = Try(label.getQryKeys(currentLine))
          qryRes match {
            case Success(qryKeys) => if (qryKeys != null && qryKeys.nonEmpty) labelQryKeysSet ++= qryKeys
            case Failure(t) => logError("Failed to execute datainterface getQryKeys" + label + ".getQryKeys")
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
          logError("= = " * 15 + " got exception in EventSource while get cache")
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
            case Failure(t) => logError("Failed to execute datainterface attachLabel" + label + ".attachLabel")
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

  /**
   * 业务处理
   */
  final def makeEvents(df: DataFrame, uniqKeys: String) = {
    println(" Begin exec evets : " + System.currentTimeMillis())

    val threadPool: ExecutorService = Executors.newCachedThreadPool

    val eventService = new ExecutorCompletionService[String](threadPool)

    val now = new java.util.Date()
    val validEvents = events.filter(event => {

      val period = event.conf.get("period", "")

      if (period.isEmpty) true
      else new EventCycleLife(period).contains(now)
    })

    validEvents.map(event => eventService.submit(new BuildEvent(event, df, uniqKeys)))

    for (index <- 0 until validEvents.size) {
      eventService.take.get()
    }

    threadPool.shutdown()

  }

}

class BuildEvent(event: Event, df: DataFrame, uniqKeys: String) extends Callable[String] {
  override def call() = {
    event.buildEvent(df, uniqKeys)
    ""
  }
}

/**
  * Use this singleton to get or register an Accumulator.
  */
object DroppedRecordsCounter {

  @volatile private var instance: Accumulator[Long] = null

  def getInstance(sc: SparkContext): Accumulator[Long] = {
    if (instance == null) {
      synchronized {
        if (instance == null) {
          instance = sc.accumulator(0L, "DroppedRecordsCounter")
        }
      }
    }
    instance
  }
}

/**
  * Use this singleton to get or register an Accumulator.
  */
object ReservedRecordsCounter {

  @volatile private var instance: Accumulator[Long] = null

  def getInstance(sc: SparkContext): Accumulator[Long] = {
    if (instance == null) {
      synchronized {
        if (instance == null) {
          instance = sc.accumulator(0L, "ReservedRecordsCounter")
        }
      }
    }
    instance
  }
}

