package com.asiainfo.ocdp.stream.config

import org.apache.spark.sql.types.StructType

import scala.beans.BeanProperty

class DataSchema extends Serializable {
  @BeanProperty var name: String = ""
  @BeanProperty var delim: String = ""
//  @BeanProperty var userFileds: String = ""
  @BeanProperty var rawSchema: StructType = null
  @BeanProperty var rawSchemaSize: Int = 0
  @BeanProperty var allItemsSchema: StructType = null
  @BeanProperty var usedItemsSchema: StructType = null
  @BeanProperty var topic : String = ""
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
  @BeanProperty var allItemsSchema: StructType = null//commonSchema + case when fields
  @BeanProperty var dataSchemas: Array[DataSchema] = null
  @BeanProperty var interval: Int = 1

  @BeanProperty var uniqKeysDelim: String = ","
  @BeanProperty var uniqKeyValuesDelim: String = "_"

  @BeanProperty var separator: String = "\\|"
  @BeanProperty var numPartitions: Int = -1

  def getTopicSet(): Set[String] = {
    dataSchemas.map(dataSchema => dataSchema.getTopic.trim).filter(!_.isEmpty).toSet
  }
}
