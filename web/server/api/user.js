"use strict";
import {jarPack, shiroConfig, trans, enableAuth} from "../config";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let User = require('../model/STREAM_USER')(sequelize, Sequelize);
let exec = require('child_process').exec;
let router = express.Router();
const prefix = "./server/lib/";

router.post('/login', function (req, res) {
  let username = req.body.username;
  let pass = req.body.password;
  exec(`java -Dconfig=${prefix}${shiroConfig} -Dtype=encrypt -Dusername=${username} -Dpassword=${pass} -jar ${prefix}${jarPack}`,
    (error, token) => {
      if(error === null){
        token = token.trim();
        if(enableAuth) {
          exec(`LC_ALL=en java -Dconfig=${prefix}${shiroConfig} -Dtype=decrypt -Dtoken=${token} -jar ${prefix}${jarPack}`,
            (err, message) => {
              if (err === null) {
                if(message.includes("Failed")) {
                  console.error("Authenticate Failed, shiro failed");
                  res.send({status: false});
                }else{
                  res.send({status: true, token});
                }
              } else {
                console.error(err);
                res.status(500).send(trans.authError);
              }
            });
        }else{
          console.error("Authenticate shiro is disabled");
          res.send({status: true, token});
        }
      }else{
        console.error(error);
        res.status(500).send(trans.authError);
      }
  });
});

router.get('/', function(req, res){
  let usertype = req.query.usertype;
  if(usertype === "admin") {
    User.findAll({attributes: ['id', 'name', 'description']}).then(function (users) {
      res.send(users);
    }).catch(function (err) {
      console.error(err);
      res.status(500).send(trans.databaseError);
    });
  }else{
    console.error("Authenticate Failed, non admin user cannot use this router");
    res.status(500).send(trans.authError);
  }
});

router.put('/', function(req, res){
  let users = req.body.users;
  let usertype = req.query.usertype;
  let _parsePassword = function (promises, i) {
    promises.push(new sequelize.Promise((resolve, reject) => {
      let user = users[i];
      if(user.password !== undefined && user.password !== null) {
        exec(`LC_ALL=en java -Dtype=sha-256 -Dusername=${user.name} -Dpassword=${user.password} -jar ${prefix}${jarPack}`,
          (error, password) => {
            if (error === null) {
              user.password = password.trim();
              if (users[i].id !== undefined && users.id !== null) {
                promises.push(User.update(users[i], {where: {id: users[i].id}}));
              } else {
                promises.push(User.create(users[i]));
              }
              resolve();
            } else {
              console.error(error);
              reject(trans.authError);
            }
          });
      }else{
        resolve();
      }
    }));
  };
  if(usertype === "admin") {
    User.findAll().then((dbUsers) => {
      let promises = [];
      for (let i in users) {
        _parsePassword(promises, i);
      }
      for(let i in dbUsers){
        let flag = true;
        for(let j in users){
          if(dbUsers[i].dataValues.id === users[j].id){
            flag = false;
            break;
          }
        }
        if(flag){
          promises.push(User.destroy({where: {id: dbUsers[i].dataValues.id}}));
        }
      }
      return sequelize.Promise.all(promises).then(function () {
        res.send({success: true});
      });
    }).catch(function(err){
      console.error(err);
      res.status(500).send(trans.databaseError);
    });
  }else{
    console.error("Authenticate Failed, non admin user cannot use this router");
    res.status(500).send(trans.authError);
  }
});

router.post('/change', function(req, res){
  let username = req.query.username;
  let usertype = req.query.usertype;
  let user = req.body.user;
  if(username === user.name || usertype === "admin") {
    User.find({where: {name: user.name}}).then(function (data) {
      if (data !== null && data !== undefined && data.dataValues !== undefined) {
        exec(`LC_ALL=en java -Dtype=sha-256 -Dusername=${user.name} -Dpassword=${user.password} -jar ${prefix}${jarPack}`,
          (error, password) => {
            if(error === null){
              user.password = password.trim();
              User.update(user, {where: {id: data.dataValues.id}}).then(function () {
                res.send({status: true});
              }, function () {
                res.send({status: false});
              });
            }else{
              console.error(error);
              res.status(500).send(trans.authError);
            }
          });
      } else {
        console.error(`Find user error, username ${user.name}`);
        res.send({status: false});
      }
    }).catch(function (err) {
      console.error(err);
      res.status(500).send(trans.databaseError);
    });
  }else{
    console.error("Authenticate Failed, non admin user cannot use this router");
    res.status(500).send(trans.authError);
  }
});

module.exports = router;
