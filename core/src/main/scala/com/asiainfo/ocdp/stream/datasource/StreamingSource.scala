package com.asiainfo.ocdp.stream.datasource

import com.asiainfo.ocdp.stream.common.Logging
import com.asiainfo.ocdp.stream.config.DataInterfaceConf
import com.asiainfo.ocdp.stream.constant.DataSourceConstant
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

/**
  * Created by rainday on 2/15/17.
  */
abstract class StreamingSource(ssc: StreamingContext, conf: DataInterfaceConf) extends Logging {

  val mDsConf = conf.getDsConf
  val mSSC = ssc

  def createStream() : DStream[String]
  def createStreamMulData(): DStream[(String, String)]
}

class HdfsReader(ssc: StreamingContext, conf: DataInterfaceConf) extends StreamingSource(ssc, conf) {

  final def createStream() : DStream[String] = {
    val path = mDsConf.get(DataSourceConstant.HDFS_DEFAULT_FS_KEY) + "/" + conf.get("path")
    mSSC.textFileStream(path)
  }
  final def createStreamMulData(): DStream[(String, String)] = {
    null
  }
}
