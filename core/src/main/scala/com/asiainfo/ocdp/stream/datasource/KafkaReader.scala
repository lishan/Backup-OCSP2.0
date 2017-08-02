package com.asiainfo.ocdp.stream.datasource

import com.asiainfo.ocdp.stream.common.Logging
import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.{CommonConstant, DataSourceConstant}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}


/**
  * Created by rainday on 7/24/17.
  */
class KafkaReader(ssc: StreamingContext, conf: DataInterfaceConf, kerberos_enable: Boolean) extends Logging {

  val mDsConf = conf.getDsConf
  val mSSC = ssc
  val mKerberosEnable = kerberos_enable

  val mTopicsSet = {
    if(CommonConstant.MulTopic) conf.getTopicSet()
    else conf.get(DataSourceConstant.TOPIC_KEY).split(DataSourceConstant.DELIM).map(_.trim).filter(!_.isEmpty).toSet
  }

  val mGroupId = mDsConf.get(DataSourceConstant.GROUP_ID_KEY)



  def createStreamMulData(taskConf: TaskConf): DStream[ConsumerRecord[String, String]] = {

    val protocol = if (mKerberosEnable) "SASL_PLAINTEXT" else "PLAINTEXT"
    /**
      * using 0.10.0 kafka API
      * auto.offset.reset ==> earliest, latest, none(throw exception when can not read the offset)
      * kafka store offset in replicated, partitioned log, no more store offset in zookeeper
      * after poll the data, kafka-010 KafkaUtils api will commit the offsets, i.e. AT MOST ONCE
      */
    val kafkaParams = Map[String, Object](ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "latest"
      , ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer]
      , ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer]
      , ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "true"
      , ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG -> "1000"
      , CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> protocol
      , "client.id" -> mGroupId
      , CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> mDsConf.get(DataSourceConstant.BROKER_LIST_KEY)
//      , "sasl.mechanism" -> "GSSAPI"
      , ConsumerConfig.GROUP_ID_CONFIG -> mGroupId)

    //val preferredHosts = LocationStrategies.PreferConsistent

    logInfo("Init Kafka Stream : brokers->" + mDsConf.get(DataSourceConstant.BROKER_LIST_KEY)
      + "; topic->" + mTopicsSet
      + "; protocol->" + protocol
      + " ; " + "group.id->" + mGroupId)

    KafkaUtils.createDirectStream[String, String](
      mSSC,
      LocationStrategies.PreferBrokers,
      ConsumerStrategies.Subscribe[String, String](mTopicsSet, kafkaParams))
  }
}
