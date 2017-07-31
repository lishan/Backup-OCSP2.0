"use strict";
// sequelize config
let Sequelize = require('sequelize');
let config = require('./config');
let env = config.env || 'dev';
let sequelize = new Sequelize(config[env].mysql, {
  logging: config[env].logging
});

module.exports = sequelize;
