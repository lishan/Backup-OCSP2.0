'use strict';

angular.module('ocspApp')
  .controller('EventsCenterCtrl',['$scope', '$rootScope', '$http', 'Notification', '$filter', '$q', 'moment', ($scope, $rootScope, $http, Notification, $filter, $q, moment)=>{
    $rootScope.init('cep');
    $scope.treedata = [];
    $scope.item = null;

    $scope.onSelect = function(item){
      //Clear periods when select audit type
      item.audit.periods = [{}];
    };

    function _getHistory(id) {
      $http.get("/api/history/" + id).success((data) => {
        $scope.history = data;
        $scope.hook = 0;
        if ($scope.history.length > 0) {
          $scope.history[0].active = true;
          $scope.history[0].first = true;
          if ($scope.history[0].config_data) {
            $scope.item = JSON.parse($scope.history[0].config_data);
          }
          $scope.item.note = null;
          $scope.item.version = null;
          if ($scope.history.length > 4) {
            for (let i = 4; i < $scope.history.length; i++) {
              $scope.history[i].hide = true;
            }
          }
        }
      });
    }

    $scope.changeEvent = (branch) => {
      $scope.history = null;
      $scope.item = null;
      if(branch.type === "event"){
        let id = branch.id;
        _getHistory(id);
      }
    };

    $scope.rightSlide = () => {
      if($scope.hook + 4 < $scope.history.length){
        $scope.history[$scope.hook].hide = true;
        $scope.history[$scope.hook + 4].hide = false;
        $scope.hook++;
      }
    };

    $scope.leftSlide = () => {
      if($scope.hook > 0){
        $scope.history[$scope.hook - 1].hide = false;
        if($scope.hook + 3 < $scope.history.length) {
          $scope.history[$scope.hook + 3].hide = true;
        }
        $scope.hook--;
      }
    };

    $scope.pressClick = (record)=> {
      if(!record.active) {
        if(record.config_data) {
          $scope.item = JSON.parse(record.config_data);
        }
        $scope.item.note = null;
        $scope.item.version = null;
        if ($scope.history.length > 0) {
          for (let i in $scope.history) {
            $scope.history[i].active = false;
          }
        }
        record.active = true;
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
      if(tree && tree.length > 0) {
        for (let i in tree) {
          if (tree[i].id === event.STREAM_EVENT_CEP.type) {
            if (!tree[i].children) {
              tree[i].children = [];
            }
            tree[i].children.push({
              id: event.id,
              type: "event",
              label: event.name,
            });
            return;
          } else if (tree[i].children && tree[i].children.length > 0) {
            _findNodeTree(tree[i].children, event);
          }
        }
      }
    }

    function _noLeaf(tree){
      if(tree && tree.length > 0) {
        for (let i in tree) {
          if ((!tree[i].children || tree[i].children.length === 0) && tree[i].type && tree[i].type === "type") {
            tree[i].noLeaf = true;
          } else {
            _noLeaf(tree[i].children);
          }
        }
      }
    }

    function _changeNameWhenUpdateEvent(tree, item){
      if(tree && tree.length > 0) {
        for (let i in tree) {
          if (tree[i].type === "event" && item.id === tree[i].id) {
            tree[i].label = item.name;
            return;
          } else {
            _changeNameWhenUpdateEvent(tree[i].children, item);
          }
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
          $q.all({event: $http.put("/api/event/" + $scope.item.id, {event: $scope.item}),
            history: $http.post("/api/history/event", {event: {config_data: $scope.item, note: $scope.item.note, version: $scope.item.version}})})
            .then(function(){
              _changeNameWhenUpdateEvent($scope.treedata, $scope.item);
              _getHistory($scope.item.id);
              Notification.success($filter('translate')('ocsp_web_common_026'));
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
