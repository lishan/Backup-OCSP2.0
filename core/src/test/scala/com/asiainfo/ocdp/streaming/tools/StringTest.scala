package com.asiainfo.ocdp.streaming.tools

import org.apache.commons.lang.{StringEscapeUtils, StringUtils}
import org.scalatest.FunSuite

/*
* run this class via `mvn clean test -pl core`
* */

class StringTest extends FunSuite {


  test("testMultiSplit") {
    val multiSplitLine = "@#@@#@c@#@"

    assert(multiSplitLine.split("@#@", -1).length == 4)

    assert(StringUtils.splitByWholeSeparatorPreserveAllTokens(multiSplitLine,StringEscapeUtils.unescapeJava("@#@")).length == 4)

  }

  test("testSingleSplit") {
    val singleSplitLine = ",,c,"

    assert(singleSplitLine.split(",", -1).length == 4)

    assert(StringUtils.splitByWholeSeparatorPreserveAllTokens(singleSplitLine,",").length == 4)

    val singleSplitLine2 = "||c|"

    assert(singleSplitLine2.split("\\|", -1).length == 4)

    assert(StringUtils.splitByWholeSeparatorPreserveAllTokens(singleSplitLine2,StringEscapeUtils.unescapeJava("\\|")).length == 4)

    assert(StringUtils.splitByWholeSeparatorPreserveAllTokens(singleSplitLine2,StringEscapeUtils.unescapeJava("|")).length == 4)

  }

}
