package com.asiainfo.ocdp.stream.constant

/**
 * Created by leo on 9/17/15.
 */
object DataSourceConstant {

  val KAFKA_TYPE = "kafka"
  val JDBC_TYPE = "jdbc"
  val CODIS_TYPE = "codis"

  //For kafka
  val ZK_CONNECT_KEY = "zookeeper.connect"
  val BROKER_LIST_KEY = "metadata.broker.list"
  val TOPIC_KEY = "topic"
  val DELIM = ","
  val GROUP_ID_KEY = "group.id"
  val NUM_CONSUMER_FETCGERS_KEY = "num.consumer.fetchers"

  //For kafka default value
  val GROUP_ID_DEF = "ocsp_group"

  //for recover
  val AT_MOST_ONCE = "at_most_once"
  val AT_LEAST_ONCE = "at_least_once"
  val FROM_LAST_STOP = "from_last_stop"
  val FROM_LATEST = "from_latest"

  //For Hdfs
  val HDFS_DEFAULT_FS_KEY = "fs.defaultFS"
  val HDFS_PATH_KEY = "path"

}
