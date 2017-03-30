package com.asiainfo.ocdp.stream.core1_6

import com.asiainfo.ocdp.stream.common.{FuncCore}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.streaming.StreamingContext

/**
  * Created by rainday on 3/29/17.
  */

class FuncCore1_6 extends FuncCore {

  final def toJsonDFrame(sc: SparkContext, rdd: RDD[String]): DataFrame = {
    val sqlc = SparkSQLSingleton.getInstance(sc)
    sqlc.read.json(rdd)
  }

  final def createDataFrame(sc: SparkContext, rowRDD : RDD[org.apache.spark.sql.Row], schema : org.apache.spark.sql.types.StructType) : DataFrame = {
    val sqlc = SparkSQLSingleton.getInstance(sc)
    sqlc.createDataFrame(rowRDD, schema)
  }

  final def DFrametoJsonMapPartitions(df: DataFrame)(func: Iterator[String] => Iterator[String]): RDD[String] = {
    df.toJSON.mapPartitions(func)
  }

  final def DFrametoJsonRDD(df: DataFrame): RDD[String] = {
    df.toJSON
  }
}

/*
class DataFrame1_6 extends DataFrameType {
  def JsonMapPartitions[U](func: Iterator[T] => Iterator[U]): RDD[U] = {

  }
}
*/

object SparkSQLSingleton {

  @transient  private var instance: SQLContext = _

  def getInstance(sc: SparkContext): SQLContext = {
    if (instance == null) {
      instance = new SQLContext(sc)
    }
    instance
  }
}

