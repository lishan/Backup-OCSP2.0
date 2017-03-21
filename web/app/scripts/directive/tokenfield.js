'use strict';
/**
 * Wizard directive
 */
angular.module('ocspApp').directive('tokenfield',['strService', function(strService) {
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
      let _findLabelTips = function(str, labels){
        let result = new Set();
        if(str !== undefined && str.trim() !== ""){
          result = new Set(strService.split(str));
        }
        if(labels !== undefined && labels.trim() !== ""){
          labels = JSON.parse(labels);
          for(let i in labels){
            if(labels[i].properties !== undefined) {
              let items = JSON.parse(labels[i].properties);
              if(items && items.labelItems && items.labelItems.length > 0) {
                for (let j in items.labelItems) {
                  if (!result.has(items.labelItems[j].pvalue)) {
                    result.add(items.labelItems[j].pvalue);
                  }
                }
              }
            }
          }
        }
        return [...result];
      };
      scope.bRequired = attrs !== undefined && attrs.required === 'true' ? true : false;
      let _bDisabled = attrs !== undefined && attrs.disabled === 'true' ? true : false;
      let $e = element.find('input');
      let token = {};
      // Add tips
      if(scope.autoCompleteModel !== undefined) {
        token = $e.tokenfield({
          autocomplete: {
            source: strService.split(scope.autoCompleteModel),
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
        let existingTokens = $(this).tokenfield('getTokens');
        $.each(existingTokens, function(index, token) {
          if (token.value === event.attrs.value) {
            event.preventDefault();
          }
        });
      });
      scope.$watch('ngModel', function() {
        token.tokenfield('setTokens', scope.ngModel);
      });
      if(_bDisabled) {
        element.find("input.token-input").attr('disabled',true);
      }
      token.on('tokenfield:sorttoken', function(){
        scope.$apply(function(){
          let fields = token.tokenfield('getTokens');
          let results = "";
          if (fields.length > 0){
            results = fields[0].value;
          }
          for (let i = 1; i < fields.length; i++) {
            if(fields[i].value !== undefined && fields[i].value.trim() !== "") {
              results += "," + fields[i].value;
            }
          }
          scope.ngModel = results;
        });
      });
    }
  };
}]);

