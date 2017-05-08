'use strict';

angular.module('ocspApp')
  .controller('EventsCenterCtrl',['$scope', '$rootScope', '$http', 'Notification', '$filter', '$q', 'moment', ($scope, $rootScope, $http, Notification, $filter, $q, moment)=>{
    $rootScope.init('cep');
    $scope.treedata = [];
    $scope.item = null;

    //Basic functions for page operation
    function _parseProperties(datainterface, prop, type = "output"){
      if(datainterface.delim === "\\|"){
        datainterface.delim = "|";
      }
      datainterface.inputs = [];
      if(prop !== undefined && prop !== null) {
        prop = JSON.parse(prop);
        if(prop.fields !== undefined && prop.fields.length > 0) {
          datainterface.fields = "";
          if (prop.fields.length > 0){
            datainterface.fields = prop.fields[0].pname;
          }
          for (let i = 1; i < prop.fields.length; i++) {
            if(prop.fields[i].pname !== undefined && prop.fields[i].pname.trim() !== "") {
              datainterface.fields += "," + prop.fields[i].pname;
            }
          }
        }
        if(prop.props !== undefined && prop.props.length > 0) {
          for (let i in prop.props) {
            if(prop.props[i].pname === "topic"){
              datainterface.topic = prop.props[i].pvalue;
            }
            if(prop.props[i].pname === "codisKeyPrefix"){
              datainterface.codisKeyPrefix = prop.props[i].pvalue;
            }
            else if(prop.props[i].pname === "uniqKeys"){
              datainterface.uniqueKey = prop.props[i].pvalue;
            }
          }
        }
        if(prop.sources !== undefined && prop.sources.length > 0 && type === "input") {
          for(let i = 0 ; i < prop.sources.length; i++){
            let result = {
              topic: prop.sources[i].topic,
              name: prop.sources[i].pname,
              delim: prop.sources[i].delim === "\\|"?"|":prop.sources[i].delim,
              fields: "",
              userFields: []
            };
            if(prop.sources[i].fields !== undefined && prop.sources[i].fields.length > 0) {
              if (prop.sources[i].fields.length > 0){
                result.fields = prop.sources[i].fields[0].pname;
              }
              for (let j = 1; j < prop.sources[i].fields.length; j++) {
                if(prop.sources[i].fields[j].pname !== undefined && prop.sources[i].fields[j].pname.trim() !== "") {
                  result.fields += "," + prop.sources[i].fields[j].pname;
                }
              }
            }
            if(prop.sources[i].userFields !== undefined && prop.sources[i].userFields.length > 0) {
              for (let j = 0; j < prop.sources[i].userFields.length; j++) {
                result.userFields.push({
                  name: prop.sources[i].userFields[j].pname,
                  value: prop.sources[i].userFields[j].pvalue
                });
              }
            }
            datainterface.inputs.push(result);
          }
        }
      }
    }

    function _parseDatasource(dataInterface){
      for(let i in $scope.datasources){
        if($scope.datasources[i].id === dataInterface.dsid){
          dataInterface.datasource = $scope.datasources[i];
          break;
        }
      }
    }

    $scope.onSelect = function(item){
      //Clear periods when select audit type
      item.audit.periods = [{}];
    };

    let _parseEventProps = function (item) {
      if ($scope.tasks && $scope.tasks.length > 0) {
        for (let i in $scope.tasks) {
          if ($scope.tasks[i].diid === item.diid) {
            item.task = $scope.tasks[i];
            break;
          }
        }
      }
      if (item.PROPERTIES !== undefined && item.PROPERTIES !== null) {
        item.PROPERTIES = JSON.parse(item.PROPERTIES);
        if (item.PROPERTIES.props !== undefined && item.PROPERTIES.props.length > 0) {
          for (let j in item.PROPERTIES.props) {
            if (item.PROPERTIES.props[j].pname === "userKeyIdx") {
              item.userKeyIdx = item.PROPERTIES.props[j].pvalue;
            }
            if (item.PROPERTIES.props[j].pname === "period") {
              item.PROPERTIES.props[j].pvalue = JSON.parse(item.PROPERTIES.props[j].pvalue);
              item.audit = {
                type: item.PROPERTIES.props[j].pvalue.period,
                periods: []
              };
              for (let w in item.PROPERTIES.props[j].pvalue.time) {
                let val = item.PROPERTIES.props[j].pvalue.time[w];
                if (item.audit.type === "none") {
                  item.audit.periods.push({
                    start: moment(val.begin.d + " " + val.begin.h).toDate(),
                    end: moment(val.end.d + " " + val.end.h).toDate()
                  });
                } else {
                  item.audit.periods.push({
                    s: val.begin.d,
                    d: val.end.d,
                    start: moment("2010-07-01 " + val.begin.h).toDate(),
                    end: moment("2010-07-01 " + val.end.h).toDate()
                  });
                }
              }
            }
            if (item.PROPERTIES.props.length === 1) {
              item.audit = {type: "always", periods: []};
            }
          }

          if (item.PROPERTIES.output_dis !== undefined && item.PROPERTIES.output_dis[0] !== undefined) {
            item.interval = item.PROPERTIES.output_dis[0].interval;
            item.delim = item.PROPERTIES.output_dis[0].delim;
            if (item.delim === "\\|") {
              item.delim = "|";
            }
            for (let j in $scope.output) {
              if ($scope.output[j].id === parseInt(item.PROPERTIES.output_dis[0].diid)) {
                item.output = $scope.output[j];
                _parseProperties(item.output, item.output.properties);
                _parseDatasource(item.output);
                break;
              }
            }
          }
        }
      }
    };

    $scope.changeEvent = (branch) => {
      if(branch.type === "event"){
        let id = branch.id;
        $http.get("/api/event/findId/" + id).success((data)=>{
          $scope.item = data;
          _parseEventProps($scope.item);
        });
      }
    };

    $scope.auditTypes = [
      {name: 'always', displayName: $filter('translate')('ocsp_web_streams_subscribe_type_always')},
      {name: 'none', displayName: $filter('translate')('ocsp_web_streams_subscribe_type_none')},
      {name: 'day', displayName: $filter('translate')('ocsp_web_streams_subscribe_type_day')},
      {name: 'week', displayName: $filter('translate')('ocsp_web_streams_subscribe_type_week')},
      {name: 'month', displayName: $filter('translate')('ocsp_web_streams_subscribe_type_month')}
    ];

    $scope.remove = function(array, $index){
      array.splice($index,1);
    };

    $scope.add = function (array) {
      if(array !== undefined) {
        array.push({
          status: 1,
          output: {},
          userFields: []
        });
      }
    };

    $scope.froalaOptions = {
      toolbarButtons : ['bold', 'italic', 'underline', '|',
        'align', 'formatOL', 'formatUL', '|', 'quote', 'selectAll'
      ],
      placeholderText: '请输入提交纪要',
      height: 100
    };

    function _findNodeTree(tree, event){
      if(!event.STREAM_EVENT_CEP){
        return;
      }
      for(let i in tree){
        if(tree[i].id === event.STREAM_EVENT_CEP.type){
          if(!tree[i].children){
            tree[i].children = [];
          }
          tree[i].children.push({
            id: event.id,
            type: "event",
            label: event.name,
          });
          return;
        }else if(tree[i].children && tree[i].children.length > 0){
          _findNodeTree(tree[i].children, event);
        }
      }
    }

    function _noLeaf(tree){
      for(let i in tree){
        if((!tree[i].children || tree[i].children.length === 0) && tree[i].type && tree[i].type === "type"){
          tree[i].noLeaf = true;
        }else{
          _noLeaf(tree[i].children);
        }
      }
    }

    $scope.update = function(){
      if($scope.item.id === undefined || $scope.item.id === null){
        Notification.error("Cannot update null event");
      }else{
        if ($scope.mainForm.$invalid) {
          angular.forEach($scope.mainForm.$error, function (field) {
            angular.forEach(field, function(errorField){
              errorField.$setTouched();
            });
          });
          Notification.error($filter('translate')('ocsp_web_common_032'));
        }else {
          $http.get("/api/event/findId/" + $scope.item.id).success((data)=>{
            let history = data;
            _parseEventProps(history);
            $q.all({event: $http.put("/api/event/" + $scope.item.id, {event: $scope.item}),
              history: $http.post("/api/history/event", {event: {config_data: history, note: $scope.item.note}})})
              .then(function(){
              Notification.success($filter('translate')('ocsp_web_common_026'));
            });
          });
        }
      }
    };

    $q.all({structure: $http.get('/api/typestructure'), events: $http.get('/api/event/all'), datasource: $http.get('/api/datasource'), outputinterface: $http.get('/api/datainterface/output'), streams: $http.get('/api/task')}).then((arr)=>{
      let tree = arr.structure.data;
      let events = arr.events.data;
      $scope.tasks = arr.streams.data;
      $scope.datasources = arr.datasource.data;
      $scope.output = arr.outputinterface.data;
      for(let i in events){
        _findNodeTree(tree, events[i]);
      }
      _noLeaf(tree);
      $scope.treedata = tree;
    });

  }]);
