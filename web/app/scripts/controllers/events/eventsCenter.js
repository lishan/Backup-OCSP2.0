'use strict';

angular.module('ocspApp')
  .controller('EventsCenterCtrl',['$scope', '$rootScope', '$http', 'Notification', '$filter', '$q', '$uibModal', 'moment', ($scope, $rootScope, $http, Notification, $filter, $q, $uibModal, moment)=>{
    $rootScope.init('cep');
    $scope.treedata = [];

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

    function _findTypesName(type){
      if(type.children_types){
        let types = JSON.parse(type.children_types);
        for(let i in types){
          for(let j in $scope.types){
            if(types[i] === $scope.types[j].id){
              $scope.types[j].vname = type.vname + "/" + $scope.types[j].type_name;
              _findTypesName($scope.types[j]);
              break;
            }
          }
        }
      }
    }

    function _init() {
      $q.all({
        structure: $http.get('/api/typestructure'),
        types: $http.get('/api/typestructure/all'),
        events: $http.get('/api/event/all'),
        datasource: $http.get('/api/datasource'),
        outputinterface: $http.get('/api/datainterface/output'),
        streams: $http.get('/api/task'),
        labels: $http.get('/api/label')
      }).then((arr) => {
        $scope.history = null;
        $scope.item = null;
        $scope.hook = 0;
        let tree = arr.structure.data;
        let events = arr.events.data;
        $scope.tasks = arr.streams.data;
        $scope.types = arr.types.data;
        $scope.datasources = arr.datasource.data;
        $scope.output = arr.outputinterface.data;
        $scope.inputLabels = arr.labels.data;
        for (let i in events) {
          _findNodeTree(tree, events[i]);
        }
        for(let i in $scope.types){
          if(!$scope.types[i].parent_type){
            $scope.types[i].vname = "/" + $scope.types[i].type_name;
            _findTypesName($scope.types[i]);
          }
        }
        _noLeaf(tree);
        $scope.treedata = tree;
      });
    }

    _init();

    $scope.onSelect = function(item){
      //Clear periods when select audit type
      item.audit.periods = [{}];
    };

    function _parseFields(diid, event) {
      $q.all({
        datainterface: $http.get('/api/datainterface/' + diid),
        labels: $http.get('/api/label/diid/' + diid)
      }).then(function (arr) {
        if (arr.datainterface && arr.datainterface.data) {
          let inputDi = JSON.parse(arr.datainterface.data[0].properties);
          let result = "";
          if (inputDi.fields) {
            if (inputDi.fields.length > 0) {
              result = inputDi.fields[0].pname;
            }
            for (let i = 1; i < inputDi.fields.length; i++) {
              result += "," + inputDi.fields[i].pname;
            }
          }
          event.inputFields = result;
        }
        let labels = [];
        for (let i in arr.labels.data) {
          for (let j in $scope.inputLabels) {
            if (arr.labels.data[i].label_id === $scope.inputLabels[j].id) {
              labels.push($scope.inputLabels[j]);
            }
          }
        }
        event.labels = labels;
      });
    }

    function _parseItem(record) {
      if (record && record.config_data) {
        $scope.item = JSON.parse(record.config_data);
        if (!$scope.item.audit) {
          $scope.item.audit = {type: "always", periods: []};
        }
        if ($scope.item.audit.periods && $scope.item.audit.periods.length > 0) {
          for (let i in $scope.item.audit.periods) {
            $scope.item.audit.periods[i].start = moment($scope.item.audit.periods[i].start).toDate();
            $scope.item.audit.periods[i].end = moment($scope.item.audit.periods[i].end).toDate();
          }
        }
        if($scope.item.task) {
          _parseFields($scope.item.task.diid, $scope.item);
        }
      }
      $scope.item.note = null;
      if($scope.history[0] && $scope.history[0].config_data) {
        let history = JSON.parse($scope.history[0].config_data);
        $scope.item.version = (parseInt(history.version) ? parseInt(history.version) : 0) + 1;
      }
    }

    $scope.openCreateType = ()=>{
      let modal = $uibModal.open({
        animation: true,
        ariaLabelledBy: 'modal-title-bottom',
        ariaDescribedBy: 'modal-body-bottom',
        templateUrl: 'type.html',
        size: 'md',
        backdrop: 'static',
        scope: $scope,
        controller: ['$scope', 'Notification', function($scope, Notification) {
          $scope.newType = {};
          $scope.closeModal = function(){
            modal.close();
          };
          $scope.saveType = function () {
            angular.forEach($scope.typeForm.$error, function (field) {
              angular.forEach(field, function(errorField){
                errorField.$setTouched();
              });
            });
            if($("#typeForm .ng-invalid").length === 0) {
              $http.post("/api/typestructure", {newType: $scope.newType}).then(function(){
                _init();
                modal.close();
                Notification.success($filter('translate')('ocsp_web_common_026'));
              });
            }
          };
        }]
      });
    };

    $scope.openSearchModal = ()=>{
      let modal = $uibModal.open({
        animation: true,
        ariaLabelledBy: 'modal-title-bottom',
        ariaDescribedBy: 'modal-body-bottom',
        templateUrl: 'search.html',
        size: 'lg',
        backdrop: 'static',
        scope: $scope,
        controller: ['$scope', function($scope) {
          $scope.searchItem = {};
          $scope.closeModal = function(){
            modal.close();
          };
          $scope.search = function () {
            $http.post("/api/event/search", {searchItem: $scope.searchItem}).success(function(events){
              $http.get('/api/typestructure').success(function(tree){
                for (let i in events) {
                  _findNodeTree(tree, events[i]);
                }
                _noLeaf(tree);
                $scope.$parent.treedata = tree;
                $scope.$parent.searchItem = JSON.stringify($scope.searchItem);
                modal.close();
              });
            });
          };
        }]
      });
    };

    $scope.openCreateEvent = ()=>{
      let modal = $uibModal.open({
        animation: true,
        ariaLabelledBy: 'modal-title-bottom',
        ariaDescribedBy: 'modal-body-bottom',
        templateUrl: 'event.html',
        size: 'lg',
        backdrop: 'static',
        scope: $scope,
        controller: ['$scope', 'Notification', function($scope, Notification) {
          $scope.newEvent = {
            output: {}
          };
          $scope.closeModal = function(){
            modal.close();
          };
          $scope.selectEventStream = function($item){
            _parseFields($item.diid, $scope.newEvent);
          };
          $scope.saveEvent = function () {
            angular.forEach($scope.eventForm.$error, function (field) {
              angular.forEach(field, function(errorField){
                errorField.$setTouched();
              });
            });
            if($("#eventForm .ng-invalid").length === 0) {
              if($scope.newEvent.note) {
                $scope.newEvent.note = $scope.newEvent.note.replace(/[<p>|</p>]/g, "");
                $scope.newEvent.note = `<p>${$scope.newEvent.note}</p>`;
              }
              $scope.newEvent.version = "1";
              $http.post("/api/event/", {event: $scope.newEvent}).success(function(data){
                $scope.newEvent.id = data.id;
                $http.post("/api/history/event", {event: {config_data: $scope.newEvent,
                  note: $scope.newEvent.note, version: $scope.newEvent.version}}).success(function(){
                  _init();
                  modal.close();
                  Notification.success($filter('translate')('ocsp_web_common_026'));
                });
              });
            }
          };
        }]
      });
    };

    function _getHistory(id) {
      $http.get("/api/history/" + id).success((data) => {
        $scope.history = data;
        $scope.hook = 0;
        if ($scope.history.length > 0) {
          $scope.history[0].active = true;
          $scope.history[0].first = true;
          _parseItem($scope.history[0]);
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
        _parseItem(record);
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
          if($scope.item.note) {
            $scope.item.note = $scope.item.note.replace(/[<p>|</p>]/g, "");
            $scope.item.note = `<p>${$scope.item.note}</p>`;
          }
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

    $scope.selectStream = function($item) {
      _parseFields($item.diid, $scope.item);
    };

  }]);
