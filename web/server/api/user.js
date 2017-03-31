"use strict";
import {jarPack, shiroConfig, trans, enableAuth} from "../config";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let User = require('../model/STREAM_USER')(sequelize, Sequelize);
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
          exec(`LC_ALL=en java -Dconfig=${prefix}${shiroConfig} -Dtype=decrypt -Dtoken=${token} -jar ${prefix}${jarPack}`,
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

router.get('/', function(req, res){
  let usertype = req.query.usertype;
  if(usertype === "admin") {
    User.findAll().then(function (users) {
      res.send(users);
    }, function () {
      res.status(500).send(trans.databaseError);
    });
  }else{
    res.status(500).send(trans.authError);
  }
});

router.put('/', function(req, res){
  let users = req.body.users;
  let usertype = req.query.usertype;
  if(usertype === "admin") {
    User.findAll().then((dbUsers) => {
      let promises = [];
      for (let i in users) {
        if (users[i].id !== undefined && users.id !== null) {
          promises.push(User.update(users[i], {where: {id: users[i].id}}));
        } else {
          promises.push(User.create(users[i]));
        }
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
      sequelize.Promise.all(promises).then(function () {
        res.send({success: true});
      }, function () {
        res.status(500).send(trans.databaseError);
      });
    }, () => {
      res.status(500).send(trans.databaseError);
    });
  }else{
    res.status(500).send(trans.authError);
  }
});

router.post('/change', function(req, res){
  let username = req.query.username;
  let usertype = req.query.usertype;
  let user = req.body.user;
  if(username === user.name || usertype === "admin") {
    let pass = user.oldPassword;
    User.find({where: {name: user.name, password: pass}}).then(function (data) {
      if (data !== null && data !== undefined && data.dataValues !== undefined) {
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

module.exports = router;
