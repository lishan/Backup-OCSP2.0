package com.asiainfo.ocdp.stream.common

import java.text.SimpleDateFormat

import scala.util.parsing.json.JSON
import java.util.{Calendar, Date}

/**
  * Created by yangjie9 on 2017/2/6.
  */
class EventCycleLife(jsonStr: String) {

  val period = {
    val value = JSON.parseFull(jsonStr)
    val map = value.get.asInstanceOf[Map[String,Any]]
    map.get("period").get.asInstanceOf[String]

  }

  val times: List[Period] = {

    val value = JSON.parseFull(jsonStr)

    var _times: List[Period] = List()

    val map = value.get.asInstanceOf[Map[String,Any]]

    val time = map.get("time").get.asInstanceOf[List[Map[String,Any]]]

    time.map(p => _times=(new Period(p)) :: _times)
    _times
  }

  val dm = new SimpleDateFormat("yyyy-MM-dd")
  val hm = new SimpleDateFormat("HH:mm:ss")

  def contains(now: Date): Boolean = {

    val nowTime = getTime(now)

    times.foreach(p => if(p.contains(nowTime)) return true)

    false
  }

  def getTime(date: Date):Time = {

    var nowMap = Map("h" -> hm.format(date))

    val calendar = Calendar.getInstance();

    calendar.setTime(date);

    period match {
      case "none" =>
        nowMap +=  ("d" -> dm.format(date))
      case "day"  =>
        nowMap +=  ("d" -> "0")
      case "week" =>
        nowMap +=  ("d" -> calendar.get(Calendar.DAY_OF_WEEK).toString)
      case "month" => {
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        var day = d.toString
        if(d<10) day= "0" + day
        nowMap += ("d" -> day)
      }
    }
    new Time(nowMap)
  }
}
class Period(p:Map[String,Any]){

  val begin = new Time(p.get("begin").get.asInstanceOf[Map[String,Any]])

  val end = new Time(p.get("end").get.asInstanceOf[Map[String,Any]])

  def contains(n:Time): Boolean ={begin <= n && end >= n}

}

class Time(t:Map[String,Any]) {

  val day =  t.get("d").get.asInstanceOf[String]
  val hour = t.get("h").get.asInstanceOf[String]

  def > (t:Time): Boolean ={

    val d = day.compareTo(t.day)

    if(d>0||(d==0&&hour.compareTo(t.hour)>0))
      return true

    false
  }
  def < (t:Time): Boolean ={

    val d = day.compareTo(t.day)

    d<0||(d==0&&hour.compareTo(t.hour)<0)
  }

  def == (t:Time): Boolean ={day.compareTo(t.day)==0&&hour.compareTo(t.hour)==0}

  def <= (t:Time): Boolean ={this==t || this < t}

  def >= (t:Time): Boolean ={this==t || this > t}
}

