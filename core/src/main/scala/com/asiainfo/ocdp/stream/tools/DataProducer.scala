package com.asiainfo.ocdp.stream.tools

import java.util.{Properties, Random}

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import org.apache.commons.lang3._

/**
  * run the producer as
  * java -cp "spark-streaming-kafka-assembly_2.10-1.6.0.jar:spark-assembly-1.6.0.2.4.0.0-169-hadoop2.7.1.2.4.0.0-169-6.0.0.jar:core-2.0.1.jar" com.asiainfo.ocdp.stream.tools.DataProducerClient host:6667 topic
  */
class DataProducer(brokers: String, topic: String) extends Runnable {
  /**
    * 为了简化消息的处理，将消息的格式定义如下：
    * EventID，time，imsi，imei，lac，ci
    */
  private val brokerList = brokers
  private val props = new Properties()
  props.put("metadata.broker.list", this.brokerList)
  props.put("serializer.class", "kafka.serializer.StringEncoder")
  props.put("producer.type", "async")
  private val config = new ProducerConfig(this.props)
  private val producer = new Producer[String, String](this.config)

  private val MSGNUM_MAX = 3
  private val IMSI_MAX = 10
  private val IMEI_MAX = 4
  private val LAC_MAX = 3
  private val CI_MAX = 4
  private val DATA_TYPE = 4

  private val PHONE_MAX = 100

  private val separator = '|'


  def run(): Unit = {
    val rand = new Random()
    var msgNo = 0
    val imsi = 1019400
    while (true) {
      //val msgNum = rand.nextInt(MSGNUM_MAX)
      try {
        //for (i <- 0 to msgNum) {

          //begin time & end time

          val beginTime = System.currentTimeMillis()
          val timeStr = DateFormatUtils.dateMs2Str(beginTime, "yyyyMMddHHmmssSSS")
          val msg = new StringBuilder(timeStr)
          msg.append(separator)
          val endTime = beginTime + 1000 * 60
          val endtimeStr = DateFormatUtils.dateMs2Str(endTime, "yyyyMMddHHmmssSSS")
          msg.append(endtimeStr)
          msg.append(separator)

          // phone number, imsi
          msg.append("139" + RandomStringUtils.randomNumeric(8))
          msg.append(separator)
          val long_imsi = "460009269" + RandomStringUtils.randomNumeric(7)//(imsi + rand.nextInt(IMSI_MAX)).toString

          msg.append(long_imsi)
          msg.append(separator)

          msg.append(rand.nextInt(DATA_TYPE) + 1)
          msg.append(separator)

          // start lac, cell, end lac, cell
          msg.append(RandomStringUtils.randomNumeric(5))
          msg.append(separator)
          msg.append(RandomStringUtils.randomNumeric(4))
          msg.append(separator)
          msg.append(RandomStringUtils.randomNumeric(5))
          msg.append(separator)
          msg.append(RandomStringUtils.randomNumeric(4))
          msg.append(separator)
          msg.append(separator)
          msg.append("0")
          msg.append(separator)
          msg.append("111")
          msg.append(separator)

          msg.append("137" + RandomStringUtils.randomNumeric(8))
          msg.append(separator)
          msg.append(imsi + 1)
          msg.append(separator)
          msg.append("00000000000")
          msg.append(separator)
          msg.append("1")

          if (is_long) {
            msg.append(separator)
            msg.append("2")
          }

          val flag = RandomUtils.nextInt(1, 100)

          if(flag > 80){
            msg.append(separator)
            msg.append("redundancy")
          }


          if(flag > 90){
            msg.append(separator)
          }

          //println(msg.toString())
          //send the generated message to broker
          sendMessage(msg.toString(), long_imsi)
          msgNo = msgNo + 1
        //}
      } catch {
        case e: Exception => println(e)
      }
      try {
        //sleep for 10 seconds after send a micro batch of message

        if(msgNo%10000 == 0){
          println(s"${msgNo} records have been sent...")
          Thread.sleep(10000)
        }
      } catch {
        case e: Exception => println(e)
      }
    }
  }

  def sendMessage(message: String, imsi: String) = {
    try {
      val data = new KeyedMessage[String, String](this.topic, imsi, message);
      producer.send(data);
    } catch {
      case e: Exception => println(e)
    }
  }
}

object DataProducerClient {
  def main(args: Array[String]) {
    if (args.length < 2) {
      println("Usage:DataProducer <metadata.broker.list> <topic>")
      System.exit(1)
    }
    //start the message producer thread
    new Thread(new DataProducer(args(0), args(1))).start()
  }
}
