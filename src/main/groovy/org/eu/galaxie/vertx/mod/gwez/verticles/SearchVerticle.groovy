package org.eu.galaxie.vertx.mod.gwez.verticles

import org.eu.galaxie.vertx.mod.gwez.BusAddr
import org.eu.galaxie.vertx.mod.gwez.MainVerticle
import org.vertx.groovy.platform.Verticle

class SearchVerticle extends Verticle {

    def start() {

        vertx.eventBus.registerHandler(BusAddr.SEARCH.address) { searchMessage ->
            vertx.eventBus.send(MainVerticle.BUS_NAME + '.search.local', searchMessage.body) { localResponse ->
                vertx.eventBus.publish(BusAddr.SEARCH_RESULT.address, localResponse.body)
            }
        }

        vertx.eventBus.registerHandler(MainVerticle.BUS_NAME + '.search.local') { searchMessage ->

            vertx.eventBus.send(BusAddr.SEARCH_FILE_BY_NAME.address, [query: searchMessage.body.query]) { dbResponse ->
                def searchResponse = [query: searchMessage.body.query]

                if (dbResponse.body.hits) {
                    searchResponse.hits = dbResponse.body.hits
                }

                searchMessage.reply(searchResponse)
            }
        }

        vertx.eventBus.registerHandler(BusAddr.GET_ASSEMBLY.address) { searchMessage ->

            String targetFileSha1 = searchMessage.body.sha1

            vertx.eventBus.send(BusAddr.GET_FILE_MAPPING_BY_SHA1.address, [sha1: targetFileSha1]) { dbResponse ->

                println "got response from DB: ${dbResponse.body}"

                if (dbResponse.body.hits) {
                    def fileName = dbResponse.body.hits[0].name

                    vertx.eventBus.send(BusAddr.GET_CHUNKS_MAP.address, [sha1: targetFileSha1]) { esChunksResponse ->

                        def allOrderedSha1 = esChunksResponse.body.hits.sort { it.num }.collect { it.sha1 }
                        def landingMessage = [
                                name: fileName,
                                sha1: searchMessage.body.sha1,
                                chunks: allOrderedSha1
                        ]
                        searchMessage.reply(landingMessage)
                    }
                }
            }
        }
    }
}
