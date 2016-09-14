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
  //城市,省标签前缀
  val city_sine = "city_name"
  val province_sine = "province"
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
      //如果查询不到user imsi, 则查询city_info信息,得到imsi码段的归属城市
      val cachedCity = labelQryData.getOrElse(getCityKeys(line).head,Map[String, String]())
      if (cachedCity.contains(city_sine)){
        val city = cachedCity(city_sine)
        labelMap += (LabelConstant.LABEL_CITY -> city)
      }

    }
    else{
      info_cols.foreach(labelName => {
        val labelValue = cachedUser(labelName)
        labelMap += (labelName -> labelValue)
      })
      //如果能查到user imsi,则将归属地设为陕西省
      labelMap += (LabelConstant.LABEL_CITY -> "Shaanxi")
    }

    labelMap ++= line

    (labelMap.toMap, cache)

  }

  override def getQryKeys(line: Map[String, String]): Set[String] = Set[String]("user_base_info:" + line("imsi"))

  private def getCityKeys(line: Map[String, String]): Set[String] = Set[String]("city_info:" + line("imsi").substring(0,10))

}
