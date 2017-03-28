'use strict';

/**
 * For label management main page controller
 */
angular.module('ocspApp')
  .controller('UserManagementCtrl', ['$scope', '$http', '$rootScope', '$filter', 'Notification', 'hotkeys', function ($scope, $http, $rootScope, $filter, Notification, hotkeys) {
    $rootScope.init('user');
    function init() {
      $http.get('/api/user').success(function (data) {
        $scope.users = data;
      });
    }

    if($rootScope.isAdmin()) {
      init();
    }

    $scope.user = {
      name: $rootScope.username
    };
    $scope.message = null;
    $scope.styles = null;
    $scope.checkPassword = function(){
      if($scope.user.password !== undefined && $scope.user.password2 !== undefined && $scope.user.password === $scope.user.password2){
        $scope.message = null;
        $scope.styles = null;
        return true;
      }else{
        $scope.message = $filter('translate')('ocsp_web_user_manage_006');
        $scope.styles = "redBlock";
        return false;
      }
    };
    hotkeys.bindTo($scope).add({
      combo: 'enter',
      allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
      callback: function() {
        if($rootScope.isAdmin()){
          $scope.saveUsers();
        }else {
          $scope.save();
        }
      }
    });
    $scope.save = function(){
      if($scope.mainForm.$invalid){
        angular.forEach($scope.mainForm.$error, function (field) {
          angular.forEach(field, function(errorField){
            errorField.$setTouched();
          });
        });
        Notification.error($filter('translate')('ocsp_web_common_032'));
      }else if ($scope.checkPassword()) {
        $http.post('/api/user/change', {"user": $scope.user}).success(function (data) {
          if(data.status) {
            $scope.user = {
              name: $rootScope.username
            };
            Notification.success($filter('translate')('ocsp_web_common_026'));
          }else{
            Notification.error($filter('translate')('ocsp_web_common_030'));
          }
        });
      }
    };

    $scope.saveUsers = () => {
      if ($scope.mainForm.$invalid) {
        angular.forEach($scope.mainForm.$error, function (field) {
          angular.forEach(field, function(errorField){
            errorField.$setTouched();
          });
        });
        Notification.error($filter('translate')('ocsp_web_common_032'));
      }else {
        $http.put("/api/user", {users: $scope.users}).success(function(){
          Notification.success($filter('translate')('ocsp_web_common_026'));
        });
      }
    };

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
  }]);
