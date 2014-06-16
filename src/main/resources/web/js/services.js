gwez.factory('eventbus', function ($rootScope) {
    var eb = new vertx.EventBus(window.location.protocol +
        '//' +
        window.location.hostname +
        ':' + window.location.port +
        '/eventbus');
    eb.handlersQueue = {};
    eb.messageQueue = [];

    eb.emit = function (channel, message, callback) {
        var self = this;
        if (self.readyState() == vertx.EventBus.OPEN) {
            self.send(channel, message, function (reply) {
                $rootScope.$apply(function () {
                    if (callback) {
                        callback(reply);
                    }
                });
            });
        } else {
            self.messageQueue.push({channel: channel, message: message, callback: callback});
        }
    };

    eb.on = function (channel, handler) {
        var self = this;
        if (self.readyState() == vertx.EventBus.OPEN) {
            self.registerHandler(channel, function (msg) {
                $rootScope.$apply(handler(msg));
            });
        } else {
            self.handlersQueue[channel] = handler;
        }
    };

    eb.onopen = function () {

        var waitingHandlersSet = 0;
        angular.forEach(eb.handlersQueue, function (handler, key) {
            eb.on(key, handler);
            waitingHandlersSet++;
        });
        if (waitingHandlersSet > 0) {
            console.log(waitingHandlersSet + ' waiting handler(s) registered.');
        }
        eb.handlersQueue = null;

        var waitingMessagesSent = 0;
        angular.forEach(eb.messageQueue, function (call) {
            eb.emit(call.channel, call.message, call.callback);
            waitingMessagesSent++;
        });
        if (waitingMessagesSent > 0) {
            console.log(waitingMessagesSent + ' queued message(s) sent.');
        }
        eb.waitingMessagesQueue = null;
    };

    return eb;
});

gwez.factory('uploader', function ($rootScope) {
    return {
        sendFile: function (fileDescList, index, progressCallback) {
            if (index >= fileDescList.length) {
                return;
            }

            self = this;
            if (fileDescList[index].status == FileStatus.SELECTED) {
                var xhr = new XMLHttpRequest();
                xhr.upload.addEventListener('progress', function (e) {
                    if (e.lengthComputable) {
                        var progress = Math.round((e.loaded * 100) / e.total);
                        $rootScope.$apply(function () {
                            progressCallback(index, progress, progressCallback);
                        });
                    }
                }, false);

                fileDescList[index].progress = 0;
                fileDescList[index].status = FileStatus.UPLOADING;

                xhr.open('PUT', '/upload?filename=' + fileDescList[index].name);
                xhr.send(fileDescList[index].file);
            } else {
                self.sendFile(fileDescList, ++index, progressCallback);
            }
        }
    };
});
