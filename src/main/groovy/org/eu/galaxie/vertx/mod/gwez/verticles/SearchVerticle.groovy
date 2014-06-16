package org.eu.galaxie.vertx.mod.gwez.verticles

import org.vertx.groovy.platform.Verticle

class SearchVerticle extends Verticle {

    def start() {

        vertx.eventBus.registerHandler('search') { searchMessage ->
            vertx.eventBus.send('search.local', searchMessage.body) { localResponse ->
                vertx.eventBus.publish('search.response', localResponse.body)
            }
        }

        vertx.eventBus.registerHandler('search.local') { searchMessage ->

            vertx.eventBus.send('org.eu.galaxie.vertx.mod.gwez.db.search', [query: searchMessage.body.query]) { dbResponse ->
                def searchResponse = [query: searchMessage.body.query]

                if (dbResponse.body.hits) {
                    searchResponse.hits = dbResponse.body.hits
                }

                searchMessage.reply(searchResponse)
            }
        }

        vertx.eventBus.registerHandler('search.get.assembly') { searchMessage ->

            String targetFileSha1 = searchMessage.body.sha1

            vertx.eventBus.send('org.eu.galaxie.vertx.mod.gwez.db.getBySha1', [sha1: targetFileSha1]) { dbResponse ->

                println "got response from DB: ${dbResponse.body}"

                if (dbResponse.body.hits) {
                    def fileName = dbResponse.body.hits[0].name

                    vertx.eventBus.send('org.eu.galaxie.vertx.mod.gwez.db.getChunkMap', [sha1: targetFileSha1]) { esChunksResponse ->

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
