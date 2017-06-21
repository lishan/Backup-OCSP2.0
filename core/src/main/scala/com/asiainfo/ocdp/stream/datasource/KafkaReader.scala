package com.asiainfo.ocdp.stream.datasource

import com.asiainfo.ocdp.stream.common.KafkaCluster
import com.asiainfo.ocdp.stream.constant.CommonConstant
import com.asiainfo.ocdp.stream.common.KafkaCluster.LeaderOffset
import com.asiainfo.ocdp.stream.config.{DataInterfaceConf, TaskConf}
import com.asiainfo.ocdp.stream.constant.{CommonConstant, DataSourceConstant}
import kafka.common.TopicAndPartition
import kafka.message.MessageAndMetadata
import kafka.serializer.StringDecoder
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.{HasOffsetRanges, KafkaUtils}

/**
  * Created by rainday on 2/14/17.
  */
class KafkaReader(ssc: StreamingContext, conf: DataInterfaceConf) extends StreamingSource(ssc, conf) {

  val mTopicsSet = {
    if(CommonConstant.MulTopic) conf.getTopicSet()
    else conf.get(DataSourceConstant.TOPIC_KEY).split(DataSourceConstant.DELIM).toSet
  }
  val mKafkaParams = Map[String, String](DataSourceConstant.BROKER_LIST_KEY -> mDsConf.get(DataSourceConstant.BROKER_LIST_KEY)
            , "auto.offset.reset" -> "largest")
  val mGroupId = mDsConf.get(DataSourceConstant.GROUP_ID_KEY)
  val mKC = new KafkaCluster(mKafkaParams)

  final def createStreamMulData(taskConf: TaskConf): DStream[(String, MessageAndMetadata[String, String])] = {

    val partitionsE = mKC.getPartitions(mTopicsSet)
    logInfo("Init Direct Kafka Stream : brokers->" + mDsConf.get(DataSourceConstant.BROKER_LIST_KEY)
      + "; topic->" + mTopicsSet + " ! " + "group.id->" + mGroupId)

    if (partitionsE.isLeft)
      throw new Exception(s"get kafka partition failed: ${partitionsE.left.get}")
    val partitions = partitionsE.right.get

    val consumerOffsets = (if(taskConf.recovery_mode == DataSourceConstant.FROM_LAST_STOP) {
      val consumerOffsetsE = mKC.getConsumerOffsets(mGroupId, partitions)
      //TestOffsets(mKC, mKafkaParams, mTopicsSet)
      CheckOffsets(mKC, mTopicsSet, mGroupId)
      (if (consumerOffsetsE.isLeft) {
        logWarning("Init Direct Kafka Stream: Failed to get Consumer offset! Use the latest data!")
        getFromOffsets(mKC, mKafkaParams, mTopicsSet)
      } else {
        consumerOffsetsE.right.get
      })
    } else {
      getFromOffsets(mKC, mKafkaParams, mTopicsSet)
    })

    consumerOffsets.foreach{ case (tp, lo) =>
      logInfo("using offset : " + lo)
    }

    KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder, (String, MessageAndMetadata[String, String])](
      mSSC, mKafkaParams, consumerOffsets, (m: MessageAndMetadata[String, String]) => (m.topic, m))
  }

  final def createStream(taskConf: TaskConf): DStream[String] = {

    val partitionsE = mKC.getPartitions(mTopicsSet)
    logInfo("Init Direct Kafka Stream : brokers->" + mDsConf.get(DataSourceConstant.BROKER_LIST_KEY)
      + "; topic->" + mTopicsSet + " ! " + "group.id->" + mGroupId)

    if (partitionsE.isLeft)
      throw new Exception(s"get kafka partition failed: ${partitionsE.left.get}")
    val partitions = partitionsE.right.get

    val consumerOffsets = (if(taskConf.recovery_mode == DataSourceConstant.FROM_LAST_STOP) {
      val consumerOffsetsE = mKC.getConsumerOffsets(mGroupId, partitions)
      //TestOffsets(mKC, mKafkaParams, mTopicsSet)
      CheckOffsets(mKC, mTopicsSet, mGroupId)
      (if (consumerOffsetsE.isLeft) {
        logWarning("Init Direct Kafka Stream: Failed to get Consumer offset! Use the latest data!")
        getFromOffsets(mKC, mKafkaParams, mTopicsSet)
      } else {
        consumerOffsetsE.right.get
      })
    } else {
      getFromOffsets(mKC, mKafkaParams, mTopicsSet)
    })

    KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder, String](
      mSSC, mKafkaParams, consumerOffsets, (m: MessageAndMetadata[String, String]) => m.message())
  }


  def TestOffsets(
      kc: KafkaCluster,
      kafkaParams: Map[String, String],
      topics: Set[String]
    ): Unit = {
    val result = for {
      topicPartitions <- kc.getPartitions(topics).right
      earliyLeaderOffsets <- (
        kc.getEarliestLeaderOffsets(topicPartitions)
      ).right
      laststLeaderOffsets <- (
        kc.getLatestLeaderOffsets(topicPartitions)
      ).right
    } yield {
      earliyLeaderOffsets.map { case (tp, lo) =>
          (tp, lo.offset)
          logInfo("earliest leaderoffset : " + lo.offset)
      }
      laststLeaderOffsets.map { case (tp, lo) =>
          (tp, lo.offset)
          logInfo("lastest leaderoffset : " + lo.offset)
      }
    }
  }

  /**we only use this function to get latest offset*/
  def getFromOffsets(
      kc: KafkaCluster,
      kafkaParams: Map[String, String],
      topics: Set[String]
    ): Map[TopicAndPartition, Long] = {
    val reset = kafkaParams.get("auto.offset.reset").map(_.toLowerCase)
    val result = for {
      topicPartitions <- kc.getPartitions(topics).right
      leaderOffsets <- (if (reset == Some("smallest")) {
        kc.getEarliestLeaderOffsets(topicPartitions)
      } else {
        kc.getLatestLeaderOffsets(topicPartitions)
      }).right
    } yield {
      leaderOffsets.map { case (tp, lo) =>
          (tp, lo.offset)
      }
    }
    KafkaCluster.checkErrors(result)
  }

  private def CheckOffsets(kc: KafkaCluster, topics: Set[String], mGroupId: String): Unit = {
    topics.foreach(topic => {
      val partitionsE = kc.getPartitions(Set(topic))
      if (partitionsE.isLeft)
        throw new Exception(s"get kafka partition failed: ${partitionsE.left.get}")

      val partitions = partitionsE.right.get
      val consumerOffsetsE = kc.getConsumerOffsets(mGroupId, partitions)

      if (consumerOffsetsE.isRight) {

        val earliestOffsetsE = kc.getEarliestLeaderOffsets(partitions)
        val earliestOffsets =
          if (earliestOffsetsE.isRight)
            earliestOffsetsE.right.get
          else
            throw new Exception(s"get earliest leader offsets failed: ${earliestOffsetsE.left.get}")

        val res = consumerOffsetsE.right.get.filter({ case (tp, n) =>
          val off = earliestOffsets(tp).offset
          off > n
        })

        if (!res.isEmpty)
          kc.setConsumerOffsets(mGroupId, res)
      }
    })
  }
}

