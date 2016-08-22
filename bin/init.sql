-- MySQL dump 10.13  Distrib 5.1.73, for redhat-linux-gnu (x86_64)
--
-- Host: localhost    Database: ocsp
-- ------------------------------------------------------
-- Server version	5.1.73

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `STREAM_DATAINTERFACE`
--

DROP TABLE IF EXISTS `STREAM_DATAINTERFACE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_DATAINTERFACE` (
  `id` varchar(16) NOT NULL,
  `name` varchar(30) NOT NULL,
  `type` int(11) NOT NULL,
  `dsid` varchar(16) NOT NULL,
  `filter_expr` text,
  `description` text,
  `delim` varchar(10) NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `properties` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dsid` (`dsid`,`name`),
  CONSTRAINT `STREAM_DATAINTERFACE_ibfk_1` FOREIGN KEY (`dsid`) REFERENCES `STREAM_DATASOURCE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_DATAINTERFACE`
--

LOCK TABLES `STREAM_DATAINTERFACE` WRITE;
/*!40000 ALTER TABLE `STREAM_DATAINTERFACE` DISABLE KEYS */;
INSERT INTO `STREAM_DATAINTERFACE` VALUES ('1','di1_input_kafka_topic_stream1',0,'1','',NULL,',',1,'{    \"props\": [        {            \"pname\": \"topic\",            \"pvalue\": \"input1\"        },        {            \"pname\": \"direct_kafka_api_flag\",            \"pvalue\": \"true\"        },        {            \"pname\": \"batch.duration.seconds\",            \"pvalue\": \"15\"        },        {            \"pname\": \"field.numbers\",            \"pvalue\": \"16\"        },        {            \"pname\": \"uniqKeys\",            \"pvalue\": \"imsi\"        },        {            \"pname\": \"UKSeparator\",            \"pvalue\": \"#\"        },        {            \"pname\": \"num.consumer.fetchers\",            \"pvalue\": \"4\"        },        {            \"pname\": \"batchSize\",            \"pvalue\": \"100\"        }    ],    \"fields\": [        {            \"pname\": \"timestamp\",            \"ptype\": \"string\"        },        {            \"pname\": \"endtime\",            \"ptype\": \"string\"        },        {            \"pname\": \"phoneNum\",            \"ptype\": \"String\"        },        {            \"pname\": \"imsi\",            \"ptype\": \"String\"        },        {            \"pname\": \"dataType\",            \"ptype\": \"String\"        },        {            \"pname\": \"lac\",            \"ptype\": \"String\"        },        {            \"pname\": \"cell\",            \"ptype\": \"String\"        },        {            \"pname\": \"endlac\",            \"ptype\": \"String\"        },        {            \"pname\": \"endcell\",            \"ptype\": \"String\"        },        {            \"pname\": \"bitholding1\",            \"ptype\": \"String\"        },        {            \"pname\": \"bitholding2\",            \"ptype\": \"String\"        },        {            \"pname\": \"bitholding3\",            \"ptype\": \"String\"        },        {            \"pname\": \"toPhoneNum\",            \"ptype\": \"String\"        },        {            \"pname\": \"bitholding4\",            \"ptype\": \"String\"        },        {            \"pname\": \"bitholding5\",            \"ptype\": \"String\"        },        {            \"pname\": \"bitholding6\",            \"ptype\": \"String\"        }    ],    \"userFields\": []}'),('2','di2_output_kafka_zhujiayu',1,'1','imsi != \"\"',NULL,',',1,'{    \"props\": [        {            \"pname\": \"topic\",            \"pvalue\": \"output1\"        },        {            \"pname\": \"direct_kafka_api_flag\",            \"pvalue\": \"true\"        },        {            \"pname\": \"batch.duration.seconds\",            \"pvalue\": \"15\"        },        {            \"pname\": \"field.numbers\",            \"pvalue\": \"17\"        },        {            \"pname\": \"uniqKeys\",            \"pvalue\": \"imsi\"        },        {            \"pname\": \"UKSeparator\",            \"pvalue\": \"#\"        },        {            \"pname\": \"num.consumer.fetchers\",            \"pvalue\": \"4\"        },        {            \"pname\": \"batchSize\",            \"pvalue\": \"100\"        }    ],    \"fields\": [        {            \"pname\": \"eventId\",            \"ptype\": \"Int\"        },        {            \"pname\": \"timestamp\",            \"ptype\": \"string\"        },        {            \"pname\": \"latestTimestamp\",            \"ptype\": \"string\"        },        {            \"pname\": \"imsi\",            \"ptype\": \"String\"        },        {            \"pname\": \"tour_area\",            \"ptype\": \"String\"        },        {            \"pname\": \"lac\",            \"ptype\": \"String\"        },        {            \"pname\": \"cell\",            \"ptype\": \"String\"        }    ],    \"userFields\": []}'),('3','output_codis',1,'2','imsi != \"\"',NULL,'',1,'{\"props\": [{\"pname\": \"codisKeyPrefix\",\"pvalue\": \"siteposition\"},{\"pname\": \"batch.duration.seconds\",\"pvalue\": \"15\"},{\"pname\": \"field.numbers\",\"pvalue\": \"17\"},{\"pname\": \"uniqKeys\",\"pvalue\": \"imsi\"},{\"pname\": \"UKSeparator\",\"pvalue\": \"#\"},{\"pname\": \"num.consumer.fetchers\",\"pvalue\": \"4\"},{\"pname\": \"batchSize\",\"pvalue\": \"100\"}],\"fields\": [{\"pname\": \"timestamp\",\"ptype\": \"string\"},{\"pname\": \"latestTimestamp\",\"ptype\": \"string\"},{\"pname\": \"imsi\",\"ptype\": \"String\"},{\"pname\": \"tour_area\",\"ptype\": \"String\"},{\"pname\": \"lac\",\"ptype\": \"String\"},{\"pname\": \"cell\",\"ptype\": \"String\"}],\"userFields\": []}');
/*!40000 ALTER TABLE `STREAM_DATAINTERFACE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STREAM_DATASOURCE`
--

DROP TABLE IF EXISTS `STREAM_DATASOURCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_DATASOURCE` (
  `id` varchar(16) NOT NULL,
  `name` varchar(30) NOT NULL,
  `type` varchar(20) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `properties` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_DATASOURCE`
--

LOCK TABLES `STREAM_DATASOURCE` WRITE;
/*!40000 ALTER TABLE `STREAM_DATASOURCE` DISABLE KEYS */;
INSERT INTO `STREAM_DATASOURCE` VALUES ('1','ds1_kafka','kafka',NULL,'[{\"pname\":\"zookeeper.connect\",\"pvalue\":\"ochadoop02:2181\"},{\"pname\":\"metadata.broker.list\",\"pvalue\":\"ochadoop02:6667\"}]'),('2','ds2_codis','codis',NULL,'[{\"pname\":\"cacheServers\",\"pvalue\":\"ochadoop04:6379\"}]');
/*!40000 ALTER TABLE `STREAM_DATASOURCE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STREAM_EVENT`
--

DROP TABLE IF EXISTS `STREAM_EVENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_EVENT` (
  `id` varchar(16) NOT NULL,
  `name` varchar(30) NOT NULL,
  `diid` varchar(16) NOT NULL,
  `select_expr` text,
  `filter_expr` text,
  `p_event_id` varchar(16) DEFAULT NULL,
  `PROPERTIES` text,
  `status` int(11) NOT NULL DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `diid` (`diid`,`name`),
  CONSTRAINT `STREAM_EVENT_ibfk_1` FOREIGN KEY (`diid`) REFERENCES `STREAM_DATAINTERFACE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_EVENT`
--

LOCK TABLES `STREAM_EVENT` WRITE;
/*!40000 ALTER TABLE `STREAM_EVENT` DISABLE KEYS */;
INSERT INTO `STREAM_EVENT` VALUES ('1','event1','1','imsi,lac,cell,full_path,tour_area,timestamp,latestTimestamp,timestampstr','imsi=4600092691019400','1','{\"props\":[{\"pname\":\"userKeyIdx\", \"pvalue\":\"2\"}],\"output_dis\":[{\"diid\":\"2\",\"interval\": \"1\",\"delim\" :\",\"}]}',1,'a'),('2','event2','1','lac','lac = 3','2','{\"props\":[{\"pname\":\"userKeyIdx\", \"pvalue\":\"2\"}],\"output_dis\":[{\"diid\":\"2\",\"interval\": \"1\",\"delim\" :\",\"}]}',1,'b'),('3','event3','1','imsi,lac','imsi=4600092691019400','3','{\"props\":[{\"pname\":\"userKeyIdx\", \"pvalue\":\"2\"}],\"output_dis\":[{\"diid\":\"3\",\"interval\": \"1\",\"delim\" :\",\"}]}',1,NULL);
/*!40000 ALTER TABLE `STREAM_EVENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STREAM_LABEL`
--

DROP TABLE IF EXISTS `STREAM_LABEL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_LABEL` (
  `id` varchar(16) NOT NULL,
  `name` varchar(30) NOT NULL,
  `class_name` varchar(100) NOT NULL,
  `diid` varchar(16) NOT NULL,
  `p_label_id` varchar(16) DEFAULT NULL,
  `status` int(11) DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  `properties` text,
  `cache_id` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `diid` (`diid`,`name`),
  CONSTRAINT `STREAM_LABEL_ibfk_1` FOREIGN KEY (`diid`) REFERENCES `STREAM_DATAINTERFACE` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_LABEL`
--

LOCK TABLES `STREAM_LABEL` WRITE;
/*!40000 ALTER TABLE `STREAM_LABEL` DISABLE KEYS */;
INSERT INTO `STREAM_LABEL` VALUES ('1','SiteLabel','com.asiainfo.ocdp.local.shaanxiyidong.label.SiteLabel','1','',1,'','{\"props\":[{\"pname\":\"userKeyIdx\", \"pvalue\":\"2\"}],\"labelItems\":[{\"pname\":\"phone_no\", \"pvalue\":\"product_no\"}]}','1'),('2','AreaLabel','com.asiainfo.ocdp.local.shaanxiyidong.label.AreaLabel','1','',1,'','{\"props\":[{\"pname\":\"user_info_cols\", \"pvalue\":\"phone_no,user_id,phone_area\"}],\"labelItems\":[{\"pname\":\"phone_no\", \"pvalue\":\"product_no\"}]}','1'),('3','UserBaseInfoLabel','com.asiainfo.ocdp.local.shaanxiyidong.label.UserBaseInfoLabel','1','',1,'','{\"props\":[{\"pname\":\"user_info_cols\", \"pvalue\":\"phone_no,user_id,phone_area\"}],\"labelItems\":[{\"pname\":\"phone_no\", \"pvalue\":\"product_no\"}]}','1');
/*!40000 ALTER TABLE `STREAM_LABEL` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STREAM_SUBJECT`
--

DROP TABLE IF EXISTS `STREAM_SUBJECT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_SUBJECT` (
  `id` varchar(16) NOT NULL,
  `name` varchar(30) NOT NULL,
  `properties` text NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_SUBJECT`
--

LOCK TABLES `STREAM_SUBJECT` WRITE;
/*!40000 ALTER TABLE `STREAM_SUBJECT` DISABLE KEYS */;
INSERT INTO `STREAM_SUBJECT` VALUES ('1','ZhuJiaYu1','{\"props\":[\n        	{\"name\":\"userKeyIdx\", \"value\":\"2\"},\n        	{\"name\":\"class_name\", \"value\":\"\"},\n        	{\"name\":\"delaytime\", \"value\":\"1800000\"}\n        	],\n          \"events\":[\n            {\"eventId\":\"1\", \"select_expr\":\"imsi,time,labels[\'user_info\'][\'product_no\'],labels[\'area_onsite\'][\'ZHUJIAYU\'],labels[\'area_info\'][\'zhujiayu_xiaoqu_name\']\"}\n          ],\n          \"output_dis\":[\n            {\"pname\":\"diid\", \"pvalue\":\"2\"}\n          ]\n        }',1,' ');
/*!40000 ALTER TABLE `STREAM_SUBJECT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STREAM_SYSTEMPROP`
--

DROP TABLE IF EXISTS `STREAM_SYSTEMPROP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_SYSTEMPROP` (
  `id` varchar(16) NOT NULL,
  `name` varchar(30) NOT NULL,
  `value` varchar(600) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_SYSTEMPROP`
--

LOCK TABLES `STREAM_SYSTEMPROP` WRITE;
/*!40000 ALTER TABLE `STREAM_SYSTEMPROP` DISABLE KEYS */;
INSERT INTO `STREAM_SYSTEMPROP` VALUES ('1','cacheManager','CodisCacheManager',NULL),('10','batchSize','100',NULL),('11','SPARK_HOME','/usr/hdp/2.4.0.0-169/spark',NULL),('12','master','yarn-client',NULL),('13','supervise','false',NULL),('14','queue','false',NULL),('15','interface_class','com.asiainfo.ocdp.streaming.App_Interface',NULL),('16','subject_class','',NULL),('17','delaySeconds','60',NULL),('18','periodSeconds','10',NULL),('19','appJars','core-1.0-SNAPSHOT.jar',NULL),('2','CodisProxy','ochadoop04:19000',NULL),('20','jars','mysql-connector-java-5.1.34.jar,spark-streaming-kafka-assembly_2.10-1.6.0.jar,common.xml,log4j.properties,commons-pool2-2.0.jar,jedis-2.6.1.jar,ShaanxiyidongFeature-1.0-SNAPSHOT.jar',NULL),('21','cacheQryBatchSizeLimit','50',NULL),('22','jedisPoolMaxTotal','8',NULL),('23','jedisPoolMaxIdle','8',NULL),('24','jedisPoolMinIdle','0',NULL),('25','jedisPoolMEM','1800000',NULL),('26','cacheServers','ochadoop04:6379,ochadoop04:6380',NULL),('27','cacheQryTaskSizeLimit','50',NULL),('28','files','/usr/ocsp/OCDP_Stream/lib/common.xml',NULL),('3','JedisMaxTotal','1000',NULL),('4','JedisMaxIdle','300',NULL),('5','JedisMEM','600000',NULL),('6','jedisTimeOut','10000',NULL),('7','checkpoint_dir','streaming/checkpoint',NULL),('8','codisQryThreadNum','500',NULL),('9','pipeLineBatch','200',NULL);
/*!40000 ALTER TABLE `STREAM_SYSTEMPROP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STREAM_TASK`
--

DROP TABLE IF EXISTS `STREAM_TASK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STREAM_TASK` (
  `id` varchar(16) NOT NULL,
  `tid` varchar(16) NOT NULL,
  `tname` varchar(16) NOT NULL,
  `type` int(11) NOT NULL,
  `receive_interval` int(11) NOT NULL DEFAULT '5',
  `num_executors` int(11) NOT NULL DEFAULT '2',
  `driver_memory` varchar(5) NOT NULL DEFAULT '2g',
  `executor_memory` varchar(5) NOT NULL DEFAULT '2g',
  `total_executor_cores` int(11) NOT NULL DEFAULT '2',
  `queue` varchar(100) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tid` (`tid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STREAM_TASK`
--

LOCK TABLES `STREAM_TASK` WRITE;
/*!40000 ALTER TABLE `STREAM_TASK` DISABLE KEYS */;
INSERT INTO `STREAM_TASK` VALUES ('1','1','ZhuJiaYu1',1,10,10,'1g','1g',2,'default',0,NULL),('2','2','test',1,1,2,'2g','2g',2,NULL,0,NULL);
/*!40000 ALTER TABLE `STREAM_TASK` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-08-22 15:39:25
