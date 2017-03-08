'use strict';
/**
 * Wizard directive
 */
angular.module('ocspApp').directive('tokenfield',[function() {
  return {
    restrict: 'E',
    templateUrl: 'views/directive/tokenfield.html',
    transclude: true,
    replace: true,
    scope: {
      ngModel: '=',
      autoCompleteModel: "="
    },
    link: function (scope, element, attrs) {
      let _findLabelTips = function(array, labels){
        let result = new Set();
        if(array !== undefined && array.length > 0){
          result = new Set(array.split(","));
        }
        if(labels !== undefined && labels !== ""){
          labels = JSON.parse(labels);
          for(let i in labels){
            if(labels[i].properties !== undefined) {
              let items = JSON.parse(labels[i].properties);
              for (let j in items.labelItems) {
                if(!result.has(items.labelItems[j].pvalue)) {
                  result.add(items.labelItems[j].pvalue);
                }
              }
            }
          }
        }
        return [...result];
      };
      scope.bRequired = attrs !== undefined && attrs.required === true ? true : false;
      let $e = element.find('input');
      let token = {};
      // Add tips
      if(scope.autoCompleteModel !== undefined) {
        token = $e.tokenfield({
          autocomplete: {
            source: scope.autoCompleteModel.split(','),
            delay: 100
          },
          sortable: true,
          showAutocompleteOnFocus: true
        });
      }else{
        token = $e.tokenfield({
          sortable: true,
        });
      }
      attrs.$observe('labels', function(labels) {
        if(labels !== undefined && labels !== null){
          $e.data('bs.tokenfield').$input.autocomplete({source: _findLabelTips(scope.autoCompleteModel,labels)});
        }
      });
      //Disable duplicated keys
      $e.on('tokenfield:createtoken', function (event) {
        var existingTokens = $(this).tokenfield('getTokens');
        $.each(existingTokens, function(index, token) {
          if (token.value === event.attrs.value) {
            event.preventDefault();
          }
        });
      });
      scope.$watch('ngModel', function() {
        token.tokenfield('setTokens', scope.ngModel);
      });
      token.on('tokenfield:sorttoken', function(){
        scope.$apply(function(){
          var fields = token.tokenfield('getTokens');
          var results = "";
          if (fields.length > 0){
            results = fields[0].value;
          }
          for (var i = 1; i < fields.length; i++) {
            if(fields[i].value !== undefined && fields[i].value !== "") {
              results += "," + fields[i].value;
            }
          }
          scope.ngModel = results;
        });
      });
    }
  };
}]);

