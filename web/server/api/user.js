"use strict";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let User = require('../model/STREAM_USER')(sequelize, Sequelize);
let crypto = require('crypto');
let config = require('../config');
let trans = config[config.trans || 'zh'];

let router = express.Router();

router.post('/login/:name', function (req, res) {
  let pass = crypto.createHash('md5').update(req.body.pass).digest("hex");
  User.find({where: {name: req.params.name, password: pass}}).then(function(user){
    if(user === null || user === undefined){
      res.send({status: false});
    }else{
      res.send({status: true});
    }
  }, function () {
    res.status(500).send(trans.databaseError);
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
