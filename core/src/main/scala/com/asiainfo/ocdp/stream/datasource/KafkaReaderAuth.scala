package com.asiainfo.ocdp.stream.datasource

import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.{CommonConstant, DataSourceConstant}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}


/**
  * Created by rainday on 7/24/17.
  */
class KafkaReaderAuth(ssc: StreamingContext, conf: DataInterfaceConf) {

  val mDsConf = conf.getDsConf
  val mSSC = ssc

  val mTopicsSet = {
    if(CommonConstant.MulTopic) conf.getTopicSet()
    else conf.get(DataSourceConstant.TOPIC_KEY).split(DataSourceConstant.DELIM).toSet
  }

  val mGroupId = mDsConf.get(DataSourceConstant.GROUP_ID_KEY)

  val mKafkaParams = Map[String, Object]("auto.offset.reset" -> "latest"
            , "key.deserializer" -> classOf[StringDeserializer]
            , "value.deserializer" -> classOf[StringDeserializer]
            , CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> "SASL_PLAINTEXT"
            , CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> mDsConf.get(DataSourceConstant.BROKER_LIST_KEY)
            , "group.id" -> mGroupId)

  def createStreamMulData(taskConf: TaskConf): DStream[ConsumerRecord[String, String]] = {

    val preferredHosts = LocationStrategies.PreferConsistent

    KafkaUtils.createDirectStream[String, String](
        mSSC,
        preferredHosts,
        ConsumerStrategies.Subscribe[String, String](mTopicsSet, mKafkaParams))

    //.map(m => (m.topic() -> m))

  //  KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder, (String, MessageAndMetadata[String, String])](
  //    mSSC, mKafkaParams, consumerOffsets, (m: MessageAndMetadata[String, String]) => (m.topic, m))

  }
}
