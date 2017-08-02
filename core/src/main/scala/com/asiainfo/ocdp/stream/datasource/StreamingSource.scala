package com.asiainfo.ocdp.stream.datasource

import com.asiainfo.ocdp.stream.common.Logging
import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.DataSourceConstant
import kafka.message.MessageAndMetadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

/**
  * Created by rainday on 2/15/17.
  */
abstract class StreamingSource(ssc: StreamingContext, conf: DataInterfaceConf) extends Logging {

  val mDsConf = conf.getDsConf
  val mSSC = ssc

  def createStream(taskConf: TaskConf) : DStream[String]
}

class HdfsReader(ssc: StreamingContext, conf: DataInterfaceConf) extends StreamingSource(ssc, conf) {

  final def createStream(taskConf: TaskConf) : DStream[String] = {
    val path = mDsConf.get(DataSourceConstant.HDFS_DEFAULT_FS_KEY) + "/" + conf.get("path")
    mSSC.textFileStream(path)
  }
}
