"use strict";
import {jarPack, shiroConfig, enableAuth, trans} from "./config";
let exec = require('child_process').exec;

module.exports = (req, res, next) => {
  let url = req.originalUrl;
  const prefix = "./server/lib/";
  let token = req.query.token;
  if(!url.includes("/api/user/login") && enableAuth){
    exec(`LC_ALL=en java -Dconfig=${prefix}${shiroConfig} -Dtype=decrypt -Dtoken=${token} -jar ${prefix}${jarPack}`,
      (error, message) => {
        if (error === null) {
          if(message.includes("Failed")) {
            console.error(message);
            res.status(500).send(trans.authError);
          }else{
            next();
          }
        } else {
          res.status(500).send(trans.authError);
        }
      });
  }else {
    next();
  }
};
