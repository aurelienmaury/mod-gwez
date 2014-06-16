var FileStatus = {
    SELECTED: 1,
    UPLOADING: 2,
    FINISHED: 3
};

gwez.value('uploadFileList', []);

function RootCtrl($rootScope, $scope, $route, $routeParams, $location, eventbus, uploadFileList) {

    $scope.$route = $route;
    $scope.$location = $location;
    $scope.$routeParams = $routeParams;
    $scope.FileStatus = FileStatus;

    $scope.searchResults = [];
    $scope.visibleResultIndex = null;
    $scope.availableFiles = [];

    // Upload section
    $scope.uploadFileList = uploadFileList;

    $scope.nodeName = '';

    $scope.createNode = function(nodeName) {
        eventbus.emit('org.eu.galaxie.vertx.mod.gwez.db.create.node', {domain: nodeName});
        $scope.nodeName = '';
    };

    $scope.removeFile = function (index) {
        uploadFileList.splice(index, 1);
    };

    $scope.onDrop = function (file) {
        $scope.uploadFileList.push({name: file.name, status: FileStatus.SELECTED, file: file});
        $scope.$apply();
    };

    $scope.showResults = function (index) {
        $scope.visibleResultIndex = index;
        $scope.availableFiles = $scope.searchResults[index].results;
    };

    $scope.search = function (searchField) {
        $scope.searchResults.push({
            search: searchField,
            results: []
        });

        eventbus.emit('search', {query: searchField});
    };

    eventbus.on('search.response', function (remoteResponse) {
        console.log('received a message: ' + JSON.stringify(remoteResponse));

        if (null != remoteResponse.hits) {
            angular.forEach($scope.searchResults, function (openSearch, index) {
                if (openSearch.search == remoteResponse.query) {
                    var sum = openSearch.results.concat(remoteResponse.hits);
                    openSearch.results = sum;
                    $scope.showResults(index);
                }
            });
        }
    });
}
