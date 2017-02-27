package com.asiainfo.ocdp.stream.config

import org.apache.spark.sql.types.StructType

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

class DataSchema extends Serializable {
  @BeanProperty var name: String = ""
  @BeanProperty var delim: String = ""
//  @BeanProperty var userFileds: String = ""
  @BeanProperty var rawSchema: StructType = null
  @BeanProperty var rawSchemaSize: Int = 0
  @BeanProperty var allItemsSchema: StructType = null
}

class DataSchemas extends Serializable {
  /*
  override def toString() = {
    var str = ""
    confs.foreach(str += _.delim + ",")
    str
  }
  */
}

/**
 * Created by leo on 8/13/15.
 */
class DataInterfaceConf extends BaseConf {
  @BeanProperty var diid: String = ""
  @BeanProperty var name: String = ""
  @BeanProperty var diType: Int = 0
  @BeanProperty var dsConf: DataSourceConf = null
  /*
  @BeanProperty var allItemsSchema: StructType = null
//  @BeanProperty var baseSchema: StructType = null
//  @BeanProperty var baseItemsSize: Int = 0
*/
  @BeanProperty var commonSchema: StructType = null
  @BeanProperty var dataSchemas: Array[DataSchema] = null
  @BeanProperty var interval: Int = 1
}
