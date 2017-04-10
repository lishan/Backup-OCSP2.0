package com.asiainfo.ocdp.stream.admin

import java.io.FileNotFoundException
import java.sql.SQLException

import com.asiainfo.ocdp.stream.common.JDBCUtil
import java.util.jar.JarFile

import com.asiainfo.ocdp.stream.constant.TableInfoConstant

/**
  * Created by rainday on 1/5/17.
  */

object AdminCLI {

  def Usage() = {
    println("Usage: stream admin")
    println("\t -loadlabel <jar path> -user <user name>")
    println("\t \t as: ./stream admin -loadlabel ../web/uploads/LabelLibrary-2.0.1.jar -user admin")
    println("")
  }

  def LoadLabel(jarfile: String, user: String): Unit = {

    val jfile = new JarFile(jarfile)
    val files = jfile.entries()

    while (files.hasMoreElements()) {
      val jEntry = files.nextElement()
      val jname = jEntry.getName

      val suffix = ".class"

      if (jname.endsWith(suffix) && jname.indexOf("$") == -1) {
        val labelName = jname.replace("/", ".").substring(0, jname.length - suffix.length)
        val className = labelName.substring(labelName.lastIndexOf(".")).replace(".","")

        val querysql = s"select * from ${TableInfoConstant.LabelDefinitionTableName} where name='${className}'"
        val res = JDBCUtil.query(querysql)

        if (res.isEmpty) {
          val sql = s"insert into ${TableInfoConstant.LabelDefinitionTableName}(name, class_name, owner) values('${className}','${labelName}', '${user}')"
          JDBCUtil.execute(sql)
          println("load label " + labelName + "owner: " + "user " + "successfully")
        } else if (labelName.compareTo(res.head.get("class_name").get) == 0 && user.compareTo(res.head.get("owner").get) != 0) {
          println("ERROR: label already exist! class name: " + labelName + " username: " + res.head.get("owner").get)
          sys.exit(-1)
        }
      }
    }
  }

  type OptionMap = Map[Symbol, Any]

  def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
    list match {
      case Nil => map
      case "-loadlabel" :: value :: tail =>
        nextOption(map ++ Map('jarfile -> value.toString), tail)
      case "-user" :: value :: tail =>
        nextOption(map ++ Map('user -> value.toString), tail)
      case "-loadlabel" :: tail => {
        println("ERROR: no  jar path input")
        Usage()
        sys.exit(-1)
      }
      case option :: tail => {
        println("Unknown option " + option)
        Usage()
        sys.exit(-1)
      }
    }
  }

  def main(args: Array[String]): Unit = {

    if (args.length == 0) Usage()

    val arglist = args.slice(1, args.length).toList


    val options = nextOption(Map(), arglist)

    if (options.isEmpty) {
      println("Unknown option ")
      Usage()
      sys.exit(-1)
    }

    val str = options.get('jarfile).getOrElse("").asInstanceOf[String]
    val user = options.get('user).getOrElse("").asInstanceOf[String]

    if (str.isEmpty) {
      println("jarfile path must be specific")
      Usage()
      sys.exit(-1)
    }

    if (user.isEmpty) {
      println("user name must be specific")
      Usage()
      sys.exit(-1)
    }

    try {
      LoadLabel(str, user)
    } catch {
      case ex: FileNotFoundException => {
        println("ERROR: can not load jar file : " + str)
        Usage()
        sys.exit(-1)
      }
      case ex: SQLException => {
        println("SQL operation failed! please check mysql config")
        sys.exit(-1)
      }
    }
    sys.exit(0)
  }
}
