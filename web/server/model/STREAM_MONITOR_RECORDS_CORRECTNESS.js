/* jshint indent: 2 */
"use strict";
module.exports = function(sequelize, DataTypes) {
  return sequelize.define('STREAM_MONITOR_RECORDS_CORRECTNESS', {
    task_id: {
      type: DataTypes.INTEGER(16),
      allowNull: false
    },
    timestamp: {
      type: DataTypes.TIME,
      allowNull: false,
      defaultValue: sequelize.literal('CURRENT_TIMESTAMP')
    },
    reserved_records: {
      type: DataTypes.BIGINT,
      allowNull: false
    },
    dropped_records: {
      type: DataTypes.BIGINT,
      allowNull: false
    },
    archived: {
      type: DataTypes.INTEGER(11),
      allowNull: false,
      defaultValue: "0"
    },
    application_id: {
      type: DataTypes.STRING,
      allowNull: false
    }
  }, {
    createdAt: false,
    updatedAt: false,
    tableName: 'STREAM_MONITOR_RECORDS_CORRECTNESS'
  });
};
