gwez.directive('uploadbox', function (uploadFileList, $rootScope, uploader, uploadFileList) {
    return {
        restrict: 'E',
        replace: true,
        transclude: true,
        scope: {onDrop: '&'},
        templateUrl: 'ng/directives/uploadbox.html',
        link: function (scope, element, attrs) {

            var behaviorOnOver = function (e) {
                e.stopPropagation();
                e.preventDefault();
                var elem = angular.element(document.getElementById('uploadbox-label'));
                elem.addClass('label-warning');
                elem.removeClass('label-info');
            };

            var behaviorOnOut = function (e) {
                e.stopPropagation();
                e.preventDefault();
                var elem = angular.element(document.getElementById('uploadbox-label'));
                elem.removeClass('label-warning');
                elem.addClass('label-info');
            };

            element.bind('dragover', behaviorOnOver);
            element.bind('dragenter', behaviorOnOver);
            element.bind('dragleave', behaviorOnOut);

            element.bind('drop', function (e) {
                e.stopPropagation();
                e.preventDefault();
                var elem = angular.element(document.getElementById('uploadbox-label'));
                elem.removeClass('label-warning');
                elem.addClass('label-info');

                var formDiv = angular.element(document.getElementById('uploadbox-form'));
                formDiv.removeClass('hide');

                angular.forEach(e.dataTransfer.files, function (droppedFile) {
                    scope.onDrop({file: droppedFile});
                });
            });

            scope.sendAll = function () {
                console.log('sending all');
                uploader.sendFile(uploadFileList, 0, function (index, progress, continueCallback) {
                    uploadFileList[index].progress = progress;
                    console.log('upload=' + progress + ' %');
                    if (progress == 100) {
                        uploadFileList[index].status = FileStatus.FINISHED;
                        uploader.sendFile(uploadFileList, ++index, continueCallback);
                    }
                });
            };

        }
    }
});

gwez.directive('uploadfile', function () {
    return {
        restrict: 'E',
        replace: true,
        transclude: true,
        scope: true,
        templateUrl: 'ng/directives/uploadfile.html'
    }
});
