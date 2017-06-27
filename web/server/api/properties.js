"use strict";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let Prop = require('../model/STREAM_SYSTEMPROP')(sequelize, Sequelize);
let config = require('../config');
let trans = config[config.trans || 'zh'];

let router = express.Router();

router.get('/', function(req, res){
  Prop.findAll({where: {status: 1}}).then(function (properties) {
    res.send(properties);
  }).catch(function (err) {
    console.error(err);
    res.status(500).send(trans.databaseError);
  });
});

router.get('/spark', function(req,res){
  Prop.find({attributes: ['id','value'], where : {name: 'SPARK_HOME'}}).then(function (properties){
    res.send(properties);
  }).catch(function(err){
    console.error(err);
    res.status(500).send(trans.databaseError);
  });
});

router.post('/spark', function(req, res){
  let value = req.body.spark;
  sequelize.transaction(function(t) {
    return Prop.find({where : {name: 'SPARK_HOME'}}).then(function (task) {
      if(task === null || task === undefined){
        return Prop.create({value: value, status : 1, name: "SPARK_HOME"},{transaction: t});
      }else {
        let result = task.dataValues;
        result.value = value;
        return Prop.update(result, {where: {id: task.id}, transaction: t});
      }
    });
  }).then(function(){
    res.send({success: true});
  }).catch(function (err) {
    console.error(err);
    res.status(500).send(trans.databaseError);
  });
});

router.post('/', function(req, res){
  let usertype = req.query.usertype;
  if(usertype === "admin") {
    let properties = req.body.data;
    let promises = [];
    for (let i in properties) {
      promises.push(Prop.update(properties[i], {where: {id: properties[i].id}}));
    }
    sequelize.Promise.all(promises).then(function () {
      res.send({success: true});
    }).catch(function (err) {
      console.error(err);
      res.status(500).send(trans.databaseError);
    });
  }else{
    console.error("Only admin user can change system properties");
    res.status(500).send(trans.authError);
  }
});


module.exports = router;
