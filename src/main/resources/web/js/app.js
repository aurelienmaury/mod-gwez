'use strict';

var bus_name = 'org.eu.galaxie.vertx.mod.gwez';
var gwez = angular.module('gwez', ['ngRoute']);

gwez.config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/', { templateUrl: 'ng/home.html' });
    $routeProvider.otherwise({redirectTo: '/'});
}]);
