'use strict';

/**
 * For label management main page controller
 */
angular.module('ocspApp')
  .controller('SystemManagementCtrl', ['$scope', '$http', 'Notification', '$q', '$rootScope', '$filter', '$uibModal', '$ngConfirm', function ($scope, $http, Notification, $q, $rootScope, $filter, $uibModal, $ngConfirm) {
    $rootScope.init('system');
    function init() {
      $q.all({prop: $http.get('/api/prop'), datasource: $http.get('/api/datasource')}).then(function(arr){
        $scope.prop = arr.prop.data;
        $scope.datasource = arr.datasource.data;
        if($scope.datasource !== undefined && $scope.datasource.length > 0) {
          for (let i in $scope.datasource) {
            $scope.datasource[i].props = JSON.parse($scope.datasource[i].properties);
          }
        }
      });
    }

    init();

    $scope.remove = function($index){
      $scope.datasource.splice($index,1);
    };

    $scope.trans = function(str){
      return str.replace(/\./g, '_');
    };

    $scope.openSparkModal = function(){
      let modal = $uibModal.open({
        animation: true,
        ariaLabelledBy: 'modal-title-bottom',
        ariaDescribedBy: 'modal-body-bottom',
        templateUrl: 'stackedModal.html',
        size: 'lg',
        backdrop: 'static',
        scope: $scope,
        controller: ['$scope', 'Notification', function($scope, Notification) {
          $scope.closeModal = function(){
            modal.close();
          };
          $scope.saveDatasource = function () {
            if($("#mainFrame .ng-invalid").length === 0) {
              $ngConfirm({
                title: $filter('translate')('ocsp_web_common_038'),
                content: $filter('translate')('ocsp_web_common_039'),
                scope: $scope,
                buttons:{
                  ok:{
                    text: $filter('translate')("ocsp_web_common_021"),
                    action: function(){
                      $http.post("/api/datasource", {data: $scope.newDatasource}).success(function () {
                        modal.close();
                        $scope.newDatasource = {};
                        Notification.success($filter('translate')('ocsp_web_common_026'));
                        init();
                      });
                    }
                  },
                  cancel:{
                    text: $filter('translate')("ocsp_web_common_020"),
                  }
                }
              });
            }
          };
        }]
      });
    };

    $scope.save = function(){
      for (let i in $scope.datasource) {
        $scope.datasource[i].properties = JSON.stringify($scope.datasource[i].props);
      }
      $q.all({
          prop: $http.post("/api/prop", {data: $scope.prop}),
          datasource: $http.put("/api/datasource", {data: $scope.datasource})
        })
        .then(function () {
          Notification.success($filter('translate')('ocsp_web_common_026'));
        });
    };

  }]);
