"use strict";
import {jarPack, shiroConfig, trans, enableAuth} from "../config";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let User = require('../model/STREAM_USER')(sequelize, Sequelize);
let crypto = require('crypto');
let exec = require('child_process').exec;

let router = express.Router();

router.post('/login', function (req, res) {
  const prefix = "./server/lib/";
  let username = req.body.username;
  let pass = req.body.password;
  exec(`java -Dconfig=${prefix}${shiroConfig} -Dtype=encrypt -Dusername=${username} -Dpassword=${pass} -jar ${prefix}${jarPack}`,
    (error, token) => {
      if(error === null){
        token = token.trim();
        if(enableAuth) {
          exec(`java -Dconfig=${prefix}${shiroConfig} -Dtype=decrypt -Dtoken=${token} -jar ${prefix}${jarPack}`,
            (err, message) => {
              if (err === null) {
                if(message.includes("Failed")) {
                  res.send({status: false});
                }else{
                  res.send({status: true, token});
                }
              } else {
                res.status(500).send(trans.authError);
              }
            });
        }else{
          res.send({status: true, token});
        }
      }else{
        res.status(500).send(trans.authError);
      }
  });
});

router.post('/change', function(req, res){
  let admin = req.query.user;
  if(admin === "admin") {
    let user = req.body.user;
    let pass = crypto.createHash('md5').update(user.oldPassword).digest("hex");
    User.find({where: {name: user.name, password: pass}}).then(function (data) {
      if (data !== null && data !== undefined && data.dataValues !== undefined) {
        user.password = crypto.createHash('md5').update(user.password).digest("hex");
        User.update(user, {where: {id: data.dataValues.id}}).then(function () {
          res.send({status: true});
        }, function () {
          res.send({status: false});
        });
      } else {
        res.send({status: false});
      }
    }, function () {
      res.status(500).send(trans.databaseError);
    });
  }else{
    res.status(500).send(trans.authError);
  }
});

router.get('/md5/:str', function(req,res){
  res.send(crypto.createHash('md5').update(req.params.str).digest("hex"));
});

module.exports = router;
