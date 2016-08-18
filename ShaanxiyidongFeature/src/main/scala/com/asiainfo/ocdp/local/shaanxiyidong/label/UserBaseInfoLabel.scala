package com.asiainfo.ocdp.local.shaanxiyidong.label

import com.asiainfo.ocdp.stream.common.StreamingCache
import com.asiainfo.ocdp.stream.constant.LabelConstant
import com.asiainfo.ocdp.stream.label.Label
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by gengwang on 16/8/11.
  */
class UserBaseInfoLabel  extends Label{
  val logger = LoggerFactory.getLogger(this.getClass)
  //区域标签前缀
  val tour_type_sine = "tour_area"
  val security_type_sine = "security_area"
  val areaPathKey = "area_path"

  override def attachLabel(line: Map[String, String], cache: StreamingCache, labelQryData: mutable.Map[String, mutable.Map[String, String]]): (Map[String, String], StreamingCache) = {

    val labelMap = fieldsMap()

    val info_cols = conf.get("user_info_cols").split(",")


    val cachedUser = labelQryData.getOrElse(getQryKeys(line).head, Map[String, String]())

    if (cachedUser.isEmpty){
      // add label with empty string if the user does not exist
      info_cols.foreach(labelName => {
        labelMap +=  (labelName -> "")
      })
    }
    else{
      info_cols.foreach(labelName => {
        val labelValue = cachedUser(labelName)
        labelMap += (labelName -> labelValue)
      })
    }

    labelMap ++= line

    (labelMap.toMap, cache)

  }

  override def getQryKeys(line: Map[String, String]): Set[String] = Set[String]("user_base_info:" + line("imsi"))

}
