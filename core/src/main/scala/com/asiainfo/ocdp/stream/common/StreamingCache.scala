package com.asiainfo.ocdp.stream.common

/**
 * Created by tsingfu on 15/8/18.
 */
class StreamingCache {

}

class LabelProps extends StreamingCache with Serializable {
  var labelsPropList = Map[String, Map[String, String]]()
}