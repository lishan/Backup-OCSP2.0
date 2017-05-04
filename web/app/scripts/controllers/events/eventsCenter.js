'use strict';

angular.module('ocspApp')
  .controller('EventsCenterCtrl',['$scope', '$rootScope', '$http', 'Notification', '$filter', ($scope, $rootScope, $http, Notification, $filter)=>{
    $rootScope.init('cep');
    $scope.example_treedata = [{
      label: 'Languages',
      children: [
        {label: 'Jade'},
        {label: 'HandlerBars'},
        {label: 'Coffeescript'}
      ]
    }];

    $scope.viewtest = "";

    $scope.my_tree_handler = (branch) => {
      $scope.viewtest = "You select this " + branch.label;
    }

  }]);
