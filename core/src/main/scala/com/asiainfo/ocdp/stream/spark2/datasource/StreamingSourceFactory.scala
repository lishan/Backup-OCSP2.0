package com.asiainfo.ocdp.stream.spark2.datasource

import com.asiainfo.ocdp.stream.common.{KafkaCluster, Logging}
import com.asiainfo.ocdp.stream.config.DataInterfaceConf
import com.asiainfo.ocdp.stream.constant.DataSourceConstant
import com.asiainfo.ocdp.stream.datasource.{HdfsReader, KafkaReader, StreamingSource}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka.HasOffsetRanges

/**
  * Created by rainday on 2/15/17.
  */

object StreamingSourceFactory extends Logging {

  def createDataSource(ssc: StreamingContext, conf: DataInterfaceConf) : StreamingSource = {
    conf.getDsConf.getDsType match  {
      case "kafka" => new KafkaReader(ssc, conf)
      case "hdfs" => new HdfsReader(ssc, conf)
      case _ =>
        throw new Exception("EventSourceType " + conf.dsConf.getDsType + " is not support !")
    }
  }

  def updateDataSource(conf: DataInterfaceConf, rdd: RDD[String]) : Boolean = {
    conf.getDsConf.getDsType match  {
      case "kafka" => updateKafkaSource(conf, rdd)
      case "hdfs" => true
      case _ =>
        throw new Exception("EventSourceType " + conf.dsConf.getDsType + " is not support !")
    }
  }

  private def updateKafkaSource(conf: DataInterfaceConf, rdd: RDD[String]) : Boolean = {

    val offsetList = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

    val dsConf = conf.getDsConf
    val kafkaParams = Map[String, String](DataSourceConstant.BROKER_LIST_KEY -> dsConf.get(DataSourceConstant.BROKER_LIST_KEY))
    val groupId = dsConf.get(DataSourceConstant.GROUP_ID_KEY)
    val kc = new KafkaCluster(kafkaParams)

    for (offsets <- offsetList) {
      val o = kc.setConsumerOffsets(groupId, Map((offsets.topicAndPartition(), offsets.untilOffset)))
      if (o.isLeft) {
        logError(s"Error updating the offset to Kafka cluster: ${o.left.get}")
        return false
      }
    }
    return true
  }
}

