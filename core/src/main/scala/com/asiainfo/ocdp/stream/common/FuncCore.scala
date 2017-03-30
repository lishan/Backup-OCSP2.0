package com.asiainfo.ocdp.stream.common

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

/**
  * Created by rainday on 3/29/17.
  */

abstract class FuncCore {

  def toJsonDFrame(sc: SparkContext, jsonRDD: RDD[String]): DataFrame
  def createDataFrame(sc: SparkContext, rowRDD : RDD[org.apache.spark.sql.Row], schema : org.apache.spark.sql.types.StructType) : DataFrame
  def DFrametoJsonMapPartitions(df: DataFrame)(func: Iterator[String] => Iterator[String]): RDD[String]
  def DFrametoJsonRDD(df: DataFrame): RDD[String]
}

