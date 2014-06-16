package org.eu.galaxie.vertx.mod.gwez.verticles

import org.eu.galaxie.vertx.mod.gwez.warp.file.server.FileServer
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class WarpVerticle extends Verticle{

    FileServer fileServer;



    def start() {
        fileServer = new FileServer(8082)

        vertx.eventBus.registerHandler('warp.file.get')
    }

    def remoteSearch(Message message) {

        // pick 7 nodes
        // store query with contact nodes listed
    }

    def remoteResponse(Message message) {
        // match response with query store
        // check origin to validate
        // update local db with response and a trust rank (number of similar responses)
    }
}
