package com.asiainfo.ocdp.stream.core2_0

import com.asiainfo.ocdp.stream.common.FuncCore
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Created by rainday on 3/29/17.
  */
class FuncCore2_0 extends FuncCore {

  final def toJsonDFrame(sc: SparkContext, rdd: RDD[String]): DataFrame = {
    val spark = SparkSessionSingleton.getInstance(sc.getConf)
    spark.read.json(rdd)
  }

  final def createDataFrame(sc: SparkContext, rowRDD : RDD[org.apache.spark.sql.Row], schema : org.apache.spark.sql.types.StructType) : DataFrame = {

    val spark = SparkSessionSingleton.getInstance(sc.getConf)
    spark.createDataFrame(rowRDD, schema)
  }
  final def DFrametoJsonMapPartitions(df: DataFrame)(func: Iterator[String] => Iterator[String]): RDD[String] = {
    implicit val mapEncoder = org.apache.spark.sql.Encoders.kryo[String]
    df.toJSON.mapPartitions(func).rdd
  }

  final def DFrametoJsonRDD(df: DataFrame): RDD[String] = {
    df.toJSON.rdd
  }

}

object SparkSessionSingleton {

  @transient  private var instance: SparkSession = _

  def getInstance(sparkConf: SparkConf): SparkSession = {
    if (instance == null) {
      instance = SparkSession
        .builder
        .config(sparkConf)
        .getOrCreate()
    }
    instance
  }
}

