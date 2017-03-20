package com.asiainfo.ocdp.stream.tools

import java.text.SimpleDateFormat
import java.util.Properties

import com.asiainfo.ocdp.stream.common.{BroadcastManager, Logging}
import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, EventConf}
import kafka.producer.KeyedMessage
import org.apache.commons.lang.math.NumberUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SaveMode}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by surq on 12/09/15
 */
trait StreamWriter extends Serializable {
  def push(df: DataFrame, conf: EventConf, uniqKeys: String)
  def push(rdd: RDD[String], conf: EventConf, uniqKeys: String)
}

/**
 * Created by surq on 12/09/15
 */
class StreamKafkaWriter(diConf: DataInterfaceConf) extends StreamWriter with Logging{


  def push(rdd: RDD[String], conf: EventConf, uniqKeys: String)= setMessage(rdd, conf, uniqKeys).count
  def push(df: DataFrame, conf: EventConf, uniqKeys: String) = setMessage(df.toJSON, conf, uniqKeys).count

  
  // 向kafka发送数据的未尾字段加当前时间
  val dateFormat = "yyyyMMdd HH:mm:ss.SSS"
  val sdf =  new SimpleDateFormat(dateFormat)
  /**
   * 向kafka发送数据
   */
  
  def setMessage(jsonRDD: RDD[String], conf: EventConf, uniqKeys: String) = {
    
    var fildList = conf.select_expr.split(",")
    if (conf.get("ext_fields", null) != null && conf.get("ext_fields") != "") {
      val fields = conf.get("ext_fields").split(",").map(ext => (ext.split("as"))(1).trim)
      fildList = fildList ++ fields
    }
    val delim = conf.delim
    val topic = diConf.get("topic")

    val broadDiconf = BroadcastManager.getBroadDiConf()

    var numPartitions = -1

    val numPartitionsCustom = conf.get("numPartitions", "null")


    if (NumberUtils.isDigits(numPartitionsCustom)){
      numPartitions = numPartitionsCustom.toInt
    }

    if (numPartitions < 0){
      numPartitions = jsonRDD.partitions.length/10
      if(numPartitions < 1){
        numPartitions = 1
      }
    }


    logInfo(s"The number of partitions is $numPartitions")

    val resultRDD: RDD[(String, String)] = transforEvent2KafkaMessage(jsonRDD, uniqKeys).coalesce(numPartitions)
    resultRDD.mapPartitions(iter => {
      val diConf = broadDiconf.value
      val messages = ArrayBuffer[KeyedMessage[String, String]]()
      val it = iter.toList.map(line =>
        {
          val key = line._1
          val msg_json = line._2
          val msg_head = Json4sUtils.jsonStr2String(msg_json, fildList, delim)
          val msg = {
            // 加入当前msg输出时间戳
            if (conf.get("extraID").toBoolean)
              conf.id + delim + msg_head + delim + sdf.format(System.currentTimeMillis)
            else
              msg_head + delim + sdf.format(System.currentTimeMillis)
          }
          if (key == null) messages.append(new KeyedMessage[String, String](topic, msg))
          else messages.append(new KeyedMessage[String, String](topic, key, msg))
          key
        })
      val msgList = messages.toList
      if (msgList.size > 0) KafkaSendTool.sendMessage(diConf.dsConf, msgList)
      it.iterator
    })
  }

  /**
   *
   * @param jsonRDD
   * @param uniqKeys
   * @return 返回输出到kafka的(key, message)元组的数组
   */
  def transforEvent2KafkaMessage(jsonRDD: RDD[String], uniqKeys: String): RDD[(String, String)] = {

    jsonRDD.map(jsonstr => {
      val data = Json4sUtils.jsonStr2Map(jsonstr)
      val key = uniqKeys.split(",").map(data(_)).mkString(",")
      (key, jsonstr)
    })
  }
}

class StreamJDBCWriter(diConf: DataInterfaceConf) extends StreamWriter {
  def push(df: DataFrame, conf: EventConf, uniqKeys: String) {
    val dsConf = diConf.dsConf
    val jdbcUrl = dsConf.get("jdbcurl")
    val tableName = diConf.get("tablename")
    val properties = new Properties()
    properties.setProperty("user", dsConf.get("user"))
    properties.setProperty("password", dsConf.get("password"))
    properties.setProperty("rowId", "false")

    df.write.mode(SaveMode.Append).jdbc(jdbcUrl, tableName, properties)
  }

  def push(rdd: RDD[String], conf: EventConf, uniqKeys: String) = {}
}