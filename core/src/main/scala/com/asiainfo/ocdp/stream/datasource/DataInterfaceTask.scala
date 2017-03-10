package com.asiainfo.ocdp.stream.datasource

import java.util.NoSuchElementException
import java.util.concurrent._

import com.asiainfo.ocdp.stream.common.{BroadcastManager, EventCycleLife}
import com.asiainfo.ocdp.stream.config.{DataSchema, MainFrameConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.{CommonConstant, DataSourceConstant}
import com.asiainfo.ocdp.stream.event.Event
import com.asiainfo.ocdp.stream.label.LabelManager
import com.asiainfo.ocdp.stream.manager.StreamTask
import com.asiainfo.ocdp.stream.service.DataInterfaceServer
import com.asiainfo.ocdp.stream.tools._
import org.apache.commons.lang.StringUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.{Accumulator, SparkContext}

import scala.reflect.ClassTag

/**
 * Created by surq on 12/09/15
 */
class DataInterfaceTask(taskConf: TaskConf) extends StreamTask {

  val taskDiid = taskConf.getDiid
  val interval = taskConf.getReceive_interval

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
        case ex: NoSuchElementException => {
          logError(s"Can not find ${DataSourceConstant.GROUP_ID_KEY}", ex)
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

  private def toDataFrame[R: ClassTag](rdd: RDD[R], sqlc: SQLContext, totalRecordsCounter: Accumulator[Long], reservedRecordsCounter: Accumulator[Long]): DataFrame = {

    val t0 = System.currentTimeMillis()
    var mixDF : DataFrame = null
    val broadDiConf = BroadcastManager.getBroadDiConf()
    val dataSchemas = broadDiConf.value.getDataSchemas

    for (dataSchema <- dataSchemas) {

      val rowRDD = rdd.map( input => {
        transform(input, dataSchema, totalRecordsCounter, reservedRecordsCounter)
      }).collect { case Some(row) => row }

      if (rowRDD.partitions.size > 0) {
        val t1 = System.currentTimeMillis()
        println("1.kafka RDD 转换成 rowRDD 耗时 (millis):" + (t1 - t0))
        val schema = dataSchema.getRawSchema()
        val commonSchema = dataSchema.getUsedItemsSchema
        val rawFrame: DataFrame = sqlc.createDataFrame(rowRDD, schema)
        val t2 = System.currentTimeMillis
        println("2.rowRDD 转换成 DataFrame 耗时 (millis):" + (t2 - t1))

        val filter_expr = conf.get("filter_expr")
        val tDF =
          if (filter_expr != null && filter_expr.trim != "")
            rawFrame.selectExpr(commonSchema.fieldNames: _*).filter(filter_expr)
          else
            rawFrame.selectExpr(commonSchema.fieldNames: _*)

        val t3 = System.currentTimeMillis
        println("3.DataFrame 最初过滤不规则数据耗时 (millis):" + (t3 - t2))

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

    val sqlc = new SQLContext(ssc.sparkContext)

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

    //2 流数据处理
    inputStream.foreachRDD(rdd => {
      //2.1 流数据转换
      val broadDiConf = BroadcastManager.getBroadDiConf()
      val dataSchemas = broadDiConf.value.getDataSchemas
      val recover_mode = DataSourceConstant.AT_MOST_ONCE

      //2.1 流数据转换
      val mixDF = toDataFrame(rdd, sqlc, totalRecordsCounter, reservedRecordsCounter)

      if (null != mixDF) {

        val tUnion = System.currentTimeMillis()

        /**
          * update offset BEFORE output the result for AT MOST ONCE
          */
        if (recover_mode == DataSourceConstant.AT_MOST_ONCE)
          StreamingSourceFactory.updateDataSource(broadDiConf.value, rdd)

        if (labels.size > 0) {
          val labelRDD = LabelManager.execLabels(mixDF)
          val t4 = System.currentTimeMillis
          println("4.dataframe 转成rdd打标签耗时(millis):" + (t4 - tUnion))

          labelRDD.persist()
          // read.json为spark sql 动作类提交job
          val enhancedDF = sqlc.read.json(labelRDD)

          val t5 = System.currentTimeMillis
          println("5.RDD 转换成 DataFrame 耗时(millis):" + (t5 - t4))

          makeEvents(enhancedDF, conf.get("uniqKeys"))

          labelRDD.unpersist()

          println("6.所有业务营销 耗时(millis):" + (System.currentTimeMillis - t5))
        }
        else {

          val t3 = System.currentTimeMillis

          mixDF.persist()
          mixDF.count()
          val t4 = System.currentTimeMillis
          println("4.mixDF count耗时(millis):" + (t4 - t3))

          makeEvents(mixDF, conf.get("uniqKeys"))
          mixDF.unpersist()
          println("6.所有业务营销 耗时(millis):" + (System.currentTimeMillis - t4))

        }

        val droppedCount = totalRecordsCounter.value/dataSchemas.length - reservedRecordsCounter.value
        logInfo(s"Dropped ${droppedCount} records since their schema size do not matching records field size.")
        logInfo(s"Reserved ${reservedRecordsCounter.value} records successfully.")

        if (MainFrameConf.systemProps.getBoolean(MainFrameConf.MONITOR_RECORDS_CORRECTNESS_ENABLE, false)){
          MonitorUtils.outputRecordsCorrectness(taskConf.id,reservedRecordsCounter.value, droppedCount, ssc.sparkContext.applicationId)
        }
      }
      /**
        * update offset AFTER output the result for AT LEAST ONCE
        */
      if (recover_mode == DataSourceConstant.AT_LEAST_ONCE)
        StreamingSourceFactory.updateDataSource(broadDiConf.value, rdd)
    })
  }

  /**
    * 业务处理
    */
  final def makeEvents(df: DataFrame, uniqKeys: String) = {
    println(" Begin exec evets : " + System.currentTimeMillis())

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

