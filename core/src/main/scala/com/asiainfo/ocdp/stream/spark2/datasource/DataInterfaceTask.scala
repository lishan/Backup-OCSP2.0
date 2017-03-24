package com.asiainfo.ocdp.stream.spark2.datasource

import java.util.NoSuchElementException
import java.util.concurrent._

import com.asiainfo.ocdp.stream.common.{BroadcastConf, BroadcastManager, EventCycleLife, StreamingCache}
import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, DataSchema, MainFrameConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.{CommonConstant, DataSourceConstant, LabelConstant}
import com.asiainfo.ocdp.stream.service.TaskServer
import com.asiainfo.ocdp.stream.manager.StreamTask
import com.asiainfo.ocdp.stream.spark2.event.Event
import com.asiainfo.ocdp.stream.spark2.service.DataInterfaceServer
import com.asiainfo.ocdp.stream.tools._
import com.asiainfo.ocdp.stream.datasource.StreamingSourceFactory
import org.apache.commons.lang.StringUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.types.StructType
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.util.LongAccumulator
import org.apache.spark.streaming.{StreamingContext, Time}
import org.apache.spark.{Accumulator, SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}


/**
 * Created by surq on 12/09/15
 */
class DataInterfaceTask(taskConf: TaskConf) extends StreamTask {

  val taskDiid = taskConf.getDiid
  val interval = taskConf.getReceive_interval
  val taskID = taskConf.getId

  val dataInterfaceService = new DataInterfaceServer

  val conf = dataInterfaceService.getDataInterfaceInfoById(taskDiid)
  val labels = dataInterfaceService.getLabelsByIFId(taskDiid)

  conf.setInterval(interval)

  private def transform[T: ClassTag](input: T, dataSchema: DataSchema,
          totalRecordsCounter: Accumulator[Long], reservedRecordsCounter: Accumulator[Long]): Option[Row] = {

    val delim = dataSchema.getDelim
    var topic = ""
    val source = {
      if (CommonConstant.MulTopic) {
        val tup = input.asInstanceOf[(String, String)]
        topic = tup._1
        tup._2
      } else {
        input.asInstanceOf[String]
      }
    }
    val inputArr = source.split(delim, -1)

		totalRecordsCounter.add(1)

    val valid = {
      if (CommonConstant.MulTopic) topic == dataSchema.getTopic && inputArr.size == dataSchema.getRawSchemaSize
      else inputArr.size == dataSchema.getRawSchemaSize
    }

    if (valid) {
      reservedRecordsCounter.add(1)
      Some(Row.fromSeq(inputArr))
    } else {
      None
    }
  }

  private def confCheck() = {
    /*
     * check the conf in Driver
     * try to get the conf we needed
     * */

    val uniqKeys = conf.get("uniqKeys")

    if (StringUtils.isEmpty(uniqKeys))
      throw new Exception("uniqueKeys is not set!!!")

    val dsconf = conf.getDsConf
    val groupID = {
      try {
        dsconf.get(DataSourceConstant.GROUP_ID_KEY)
      } catch {
        case ex: NoSuchElementException => {
          logWarning(s"Can not find ${DataSourceConstant.GROUP_ID_KEY}")
          StringUtils.EMPTY
        }
      }
    }

    if (StringUtils.isEmpty(groupID)) {
      val def_group_name = DataSourceConstant.GROUP_ID_DEF + taskConf.getId + taskConf.getName
      logWarning("no group_id , use default : " + def_group_name)
      dsconf.set(DataSourceConstant.GROUP_ID_KEY, def_group_name)
    }
  }

  private def toDataFrame[R: ClassTag](rdd: RDD[R], totalRecordsCounter: Accumulator[Long], reservedRecordsCounter: Accumulator[Long]): DataFrame = {

    var mixDF : DataFrame = null
    val broadDiConf = BroadcastManager.getBroadDiConf()
    val dataSchemas = broadDiConf.value.getDataSchemas

    for (dataSchema <- dataSchemas) {

      val rowRDD = withUptime("1.kafka RDD 转换成 rowRDD"){
        rdd.map( input => {
          transform(input, dataSchema, totalRecordsCounter, reservedRecordsCounter)
        }).collect { case Some(row) => row }
      }

      if (rowRDD.partitions.size > 0) {

        val schema = dataSchema.getRawSchema()
        val commonSchema = dataSchema.getUsedItemsSchema
        val rawFrame: DataFrame = withUptime("2.rowRDD 转换成 DataFrame"){
          val spark = SparkSessionSingleton.getInstance(rowRDD.sparkContext.getConf)
          spark.createDataFrame(rowRDD, schema)
        }

        val filter_expr = conf.get("filter_expr")
        val tDF = withUptime("3.DataFrame 最初过滤不规则数据"){
          if (filter_expr != null && filter_expr.trim != "")
            rawFrame.selectExpr(commonSchema.fieldNames: _*).filter(filter_expr)
          else
            rawFrame.selectExpr(commonSchema.fieldNames: _*)
        }

        if (null == mixDF)
          mixDF = tDF
        else
          mixDF = mixDF.unionAll(tDF)

      } else
        println("当前时间片内正确输入格式的流数据为空, 不做任何处理.")
    }
    mixDF
  }

  final def process(ssc: StreamingContext) = {

    // check the config first
    confCheck()

    //1 根据输入数据接口配置，生成数据流 DStream

    val dataSource = StreamingSourceFactory.createDataSource(ssc, conf)
    val inputStream = {
      if (CommonConstant.MulTopic) dataSource.createStreamMulData()
      else dataSource.createStream()
    }

    //1.2 根据输入数据接口配置，生成构造 sparkSQL DataFrame 的 structType
    // 全量字段: baseItems + udfItems

    //Broad cast configuration from driver to executor
    BroadcastManager.broadcastDiConf(conf)
    BroadcastManager.broadcastLabels(labels)
    BroadcastManager.broadcastSystemProps(MainFrameConf.systemProps)
    BroadcastManager.broadcastCodisProps(MainFrameConf.codisProps)
    BroadcastManager.broadcastTaskConf(taskConf)

    val totalRecordsCounter = TotalRecordsCounter.getInstance(ssc.sparkContext)
    val reservedRecordsCounter = ReservedRecordsCounter.getInstance(ssc.sparkContext)

    val taskServer = new TaskServer

    //2 流数据处理
    inputStream.foreachRDD((rdd, time: Time) => {
      //2.1 流数据转换
      val broadDiConf = BroadcastManager.getBroadDiConf()
      val dataSchemas = broadDiConf.value.getDataSchemas
      val recover_mode = DataSourceConstant.AT_MOST_ONCE

      taskServer.updateHeartbeat(taskID)

      //2.1 流数据转换
      val mixDF = toDataFrame(rdd, totalRecordsCounter, reservedRecordsCounter)
      val spark = SparkSessionSingleton.getInstance(ssc.sparkContext.getConf)

      if (null != mixDF) {
        /**
          * update offset BEFORE output the result for AT MOST ONCE
          */
        if (recover_mode == DataSourceConstant.AT_MOST_ONCE)
          StreamingSourceFactory.updateDataSource(broadDiConf.value, rdd)

        if (labels.size > 0) {
          val labelDF = withUptime("4.dataframe 转成rdd打标"){
            execLabels(mixDF)
          }

          labelDF.persist()
          // read.json为spark sql 动作类提交job
          val enhancedDF = withUptime("5.RDD 转换成 DataFrame"){
            spark.read.json(labelDF.rdd)
          }

          withUptime("6.所有业务营销"){
            makeEvents(enhancedDF, conf.get("uniqKeys"))
          }

          labelDF.unpersist()
        }
        else {
          mixDF.persist()
          mixDF.count()

          withUptime("4.所有业务营销"){
            makeEvents(mixDF, conf.get("uniqKeys"))
          }
          mixDF.unpersist()
        }

        val droppedCount = totalRecordsCounter.value/dataSchemas.length - reservedRecordsCounter.value
        logInfo(s"Dropped ${droppedCount} records since their schema size do not matching records field size.")
        logInfo(s"Reserved ${reservedRecordsCounter.value} records successfully.")

        var maxMem = 0L
        var memUsed = 0L
        var memRemaining = 0L

        ssc.sparkContext.getExecutorStorageStatus.foreach(mem => {
          maxMem = maxMem + mem.maxMem
          memUsed = memUsed + mem.memUsed
          memRemaining = memRemaining + mem.memRemaining
        })

        if (MainFrameConf.systemProps.getBoolean(MainFrameConf.MONITOR_TASK_MONITOR_ENABLE, false)){
          MonitorUtils.outputTaskStatistics(taskConf.id,
            reservedRecordsCounter.value,
            droppedCount,
            ssc.sparkContext.applicationId,
            maxMem,
            memUsed,
            memRemaining,
            (System.currentTimeMillis() - time.milliseconds))
        }
      }
      /**
        * update offset AFTER output the result for AT LEAST ONCE
        */
      if (recover_mode == DataSourceConstant.AT_LEAST_ONCE)
        StreamingSourceFactory.updateDataSource(broadDiConf.value, rdd)
    })
  }

  def execLabels(df: DataFrame): Dataset[String] = {

    val broadDiConf = BroadcastManager.getBroadDiConf
    val broadLabels = BroadcastManager.getBroadLabels
    val broadSysProps = BroadcastManager.getBroadSysProps
    val broadCodisProps = BroadcastManager.getBroadCodisProps
    val broadTaskConf = BroadcastManager.getBroadTaskConf

    implicit val mapEncoder = org.apache.spark.sql.Encoders.kryo[String]
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
        busnessKeyList += (s"${LabelConstant.LABEL_CACHE_PREFIX_NAME}_${broadTaskConf.value.id}:${uk}" -> currentLine)
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

    val threadPool: ExecutorService = Executors.newCachedThreadPool

    val eventService = new ExecutorCompletionService[String](threadPool)
    val events: Array[Event] = dataInterfaceService.getEventsByIFId(taskDiid)

    val now = new java.util.Date()
    val validEvents = events.filter(event => {

      val period = event.conf.get("period", "")

      if (period.isEmpty) true
      else new EventCycleLife(period).contains(now)
    })

    validEvents.map(event => eventService.submit(new BuildEvent(event, df, uniqKeys))).foreach(_=>eventService.take.get())

    threadPool.shutdown()
  }

  def withUptime[U](description: String)(fun: => U): U ={
    val start = System.currentTimeMillis
    val result = fun
    val end = System.currentTimeMillis
    println(s"${description}耗时(millis): ${end-start}")
    result
  }
}

class BuildEvent(event: Event, df: DataFrame, uniqKeys: String) extends Callable[String] {
  override def call() = {
    event.buildEvent(df, uniqKeys)
    ""
  }
}

/** Lazily instantiated singleton instance of SparkSession */
object SparkSessionSingleton {

  @transient  private var instance: SparkSession = _

  def getInstance(sparkConf: SparkConf): SparkSession = {
    if (instance == null) {
      instance = SparkSession
        .builder
        .config(sparkConf)
        .getOrCreate()
    }
    instance
  }
}


/**
  * Use this singleton to get or register an Accumulator.
  */
object TotalRecordsCounter {

  @volatile private var instance: Accumulator[Long] = null

  def getInstance(sc: SparkContext): Accumulator[Long] = {
    if (instance == null) {
      synchronized {
        if (instance == null) {
          instance = sc.accumulator(0L, "TotalRecordsCounter")
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

