-- MySQL dump 10.13  Distrib 5.7.16, for Linux (x86_64)
--
-- Host: localhost    Database: ocsp
-- ------------------------------------------------------
-- Server version	5.7.16

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
-- Dumping data for table `STREAM_DATAINTERFACE`
--

LOCK TABLES `STREAM_DATAINTERFACE` WRITE;
/*!40000 ALTER TABLE `STREAM_DATAINTERFACE` DISABLE KEYS */;
INSERT INTO `STREAM_DATAINTERFACE` VALUES (1,'di1_input_kafka_topic_stream1',0,1,'',NULL,'\\|',1,'{\"props\":[{\"pname\":\"uniqKeys\",\"pvalue\":\"imsi\"}],\"fields\":[{\"pname\":\"timestamp\",\"ptype\":\"string\"},{\"pname\":\"endtime\",\"ptype\":\"string\"},{\"pname\":\"phoneNum\",\"ptype\":\"String\"},{\"pname\":\"imsi\",\"ptype\":\"String\"},{\"pname\":\"dataType\",\"ptype\":\"String\"},{\"pname\":\"lac\",\"ptype\":\"String\"},{\"pname\":\"cell\",\"ptype\":\"String\"},{\"pname\":\"endlac\",\"ptype\":\"String\"},{\"pname\":\"endcell\",\"ptype\":\"String\"}],\"sources\":[{\"pname\":\"3g\",\"delim\":\"\\\\|\",\"topic\":\"betainput\",\"userFields\":[{\"pname\":\"imsi2\",\"undefined\":\"on\",\"pvalue\":\"(case when dataType in(1, 2) then lac else endlac end)\"}],\"fields\":[{\"pname\":\"timestamp\",\"ptype\":\"string\"},{\"pname\":\"endtime\",\"ptype\":\"string\"},{\"pname\":\"phoneNum\",\"ptype\":\"String\"},{\"pname\":\"imsi\",\"ptype\":\"String\"},{\"pname\":\"dataType\",\"ptype\":\"String\"},{\"pname\":\"lac\",\"ptype\":\"String\"},{\"pname\":\"cell\",\"ptype\":\"String\"},{\"pname\":\"endlac\",\"ptype\":\"String\"},{\"pname\":\"endcell\",\"ptype\":\"String\"},{\"pname\":\"bitholding1\",\"ptype\":\"String\"},{\"pname\":\"bitholding2\",\"ptype\":\"String\"},{\"pname\":\"bitholding3\",\"ptype\":\"String\"},{\"pname\":\"toPhoneNum\",\"ptype\":\"String\"},{\"pname\":\"bitholding4\",\"ptype\":\"String\"},{\"pname\":\"bitholding5\",\"ptype\":\"String\"},{\"pname\":\"bitholding6\",\"ptype\":\"String\"},{\"pname\":\"bitholding7\",\"ptype\":\"String\"}]},{\"pname\":\"4g\",\"delim\":\"\\\\|\",\"topic\":\"alphainput\",\"userFields\":[{\"pname\":\"imsi2\",\"undefined\":\"on\",\"pvalue\":\"(case when dataType in(2, 3) then lac else endlac end)\"}],\"fields\":[{\"pname\":\"timestamp\",\"ptype\":\"string\"},{\"pname\":\"endtime\",\"ptype\":\"string\"},{\"pname\":\"phoneNum\",\"ptype\":\"String\"},{\"pname\":\"imsi\",\"ptype\":\"String\"},{\"pname\":\"dataType\",\"ptype\":\"String\"},{\"pname\":\"lac\",\"ptype\":\"String\"},{\"pname\":\"cell\",\"ptype\":\"String\"},{\"pname\":\"endlac\",\"ptype\":\"String\"},{\"pname\":\"endcell\",\"ptype\":\"String\"},{\"pname\":\"bitholding1\",\"ptype\":\"String\"},{\"pname\":\"bitholding2\",\"ptype\":\"String\"},{\"pname\":\"bitholding3\",\"ptype\":\"String\"},{\"pname\":\"toPhoneNum\",\"ptype\":\"String\"},{\"pname\":\"bitholding4\",\"ptype\":\"String\"},{\"pname\":\"bitholding5\",\"ptype\":\"String\"},{\"pname\":\"bitholding6\",\"ptype\":\"String\"}]}]}'),(2,'di2_output_kafka_full',1,1,'imsi != \"\"',NULL,',',1,'{    \"props\": [        {            \"pname\": \"topic\",            \"pvalue\": \"output1\"        },        {            \"pname\": \"direct_kafka_api_flag\",            \"pvalue\": \"true\"        },        {            \"pname\": \"field.numbers\",            \"pvalue\": \"17\"        },        {            \"pname\": \"uniqKeys\",            \"pvalue\": \"imsi\"        },        {            \"pname\": \"UKSeparator\",            \"pvalue\": \"#\"        },        {            \"pname\": \"num.consumer.fetchers\",            \"pvalue\": \"4\"        },        {            \"pname\": \"batchSize\",            \"pvalue\": \"100\"        }    ],    \"fields\": [        {            \"pname\": \"eventId\",            \"ptype\": \"Int\"        },        {            \"pname\": \"timestamp\",            \"ptype\": \"string\"        },        {            \"pname\": \"latestTimestamp\",            \"ptype\": \"string\"        },        {            \"pname\": \"imsi\",            \"ptype\": \"String\"        },        {            \"pname\": \"tour_area\",            \"ptype\": \"String\"        },        {            \"pname\": \"lac\",            \"ptype\": \"String\"        },        {            \"pname\": \"cell\",            \"ptype\": \"String\"        }    ],    \"userFields\": []}'),(3,'output_codis',1,2,'imsi != \"\"',NULL,'',1,'{\"props\": [{\"pname\": \"codisKeyPrefix\",\"pvalue\": \"siteposition\"},{\"pname\": \"field.numbers\",\"pvalue\": \"17\"},{\"pname\": \"uniqKeys\",\"pvalue\": \"imsi\"},{\"pname\": \"UKSeparator\",\"pvalue\": \"#\"},{\"pname\": \"num.consumer.fetchers\",\"pvalue\": \"4\"},{\"pname\": \"batchSize\",\"pvalue\": \"100\"}],\"fields\": [{\"pname\": \"timestamp\",\"ptype\": \"string\"},{\"pname\": \"latestTimestamp\",\"ptype\": \"string\"},{\"pname\": \"imsi\",\"ptype\": \"String\"},{\"pname\": \"tour_area\",\"ptype\": \"String\"},{\"pname\": \"lac\",\"ptype\": \"String\"},{\"pname\": \"cell\",\"ptype\": \"String\"}],\"userFields\": []}'),(4,'output_kafka_area',1,1,'',NULL,'',1,'{\"props\":[{\"pname\":\"topic\",\"pvalue\":\"output2\"},{\"pname\":\"direct_kafka_api_flag\",\"pvalue\":\"true\"},{\"pname\":\"field.numbers\",\"pvalue\":\"17\"},{\"pname\":\"uniqKeys\",\"pvalue\":\"imsi\"},{\"pname\":\"UKSeparator\",\"pvalue\":\"#\"},{\"pname\":\"num.consumer.fetchers\",\"pvalue\":\"4\"},{\"pname\":\"batchSize\",\"pvalue\":\"100\"}],\"fields\":[],\"userFields\":[]}');
/*!40000 ALTER TABLE `STREAM_DATAINTERFACE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `STREAM_EVENT`
--

LOCK TABLES `STREAM_EVENT` WRITE;
/*!40000 ALTER TABLE `STREAM_EVENT` DISABLE KEYS */;
INSERT INTO `STREAM_EVENT` VALUES (1,'event1',1,'imsi,cell,tour_area,isAccu_tour,security_area,isAccu_security,timestamp','full_path=\'true\'',1,'{\"props\":[{\"pname\":\"userKeyIdx\",\"pvalue\":\"2\"}],\"output_dis\":[{\"diid\":\"2\",\"interval\": 10,\"delim\" :\",\"}]}',1,'a',NULL),(3,'event3',1,'imsi,cell,timestamp,isLatestSite,tour_area,security_area,acyc_id,live_lac,area_code,work_lac,work_cellid,stat_date,serial_number,age_level,sex,eparchy_id,city_code,pspt_prov_code,pspt_eparchy_id,fee_level,city_name','isLatestSite=\'true\'',3,'{\"props\":[{\"pname\":\"userKeyIdx\",\"pvalue\":\"2\"}],\"output_dis\":[{\"diid\":\"3\",\"interval\": 0,\"delim\" :\",\"}]}',1,NULL,NULL),(4,'event4',1,'imsi,cell,tour_area,isAccu_tour,security_area,isAccu_security,timestamp','isAccu_tour=\'true\' OR isAccu_security=\'ture\'',4,'{\"props\":[{\"pname\":\"userKeyIdx\",\"pvalue\":\"2\"}],\"output_dis\":[{\"diid\":\"4\",\"interval\": 0,\"delim\" :\",\"}]}',1,NULL,NULL);
/*!40000 ALTER TABLE `STREAM_EVENT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `STREAM_TASK`
--

LOCK TABLES `STREAM_TASK` WRITE;
/*!40000 ALTER TABLE `STREAM_TASK` DISABLE KEYS */;
INSERT INTO `STREAM_TASK` VALUES (1,'Stream Demo','',1,30,10,'1g','1g',2,'default',0,NULL,NULL,NULL,0,0,1,NULL,'','from_latest');
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

