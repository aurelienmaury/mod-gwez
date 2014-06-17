package org.eu.galaxie.vertx.mod.gwez.verticles

import org.eu.galaxie.vertx.mod.gwez.BusAddr
import org.eu.galaxie.vertx.mod.gwez.MainVerticle
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.file.AsyncFile
import org.vertx.groovy.core.http.HttpServer
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.core.streams.Pump
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.file.impl.PathAdjuster
import org.vertx.java.core.impl.VertxInternal

class WebVerticle extends Verticle {

    private static final String DEFAULT_INDEX = 'index.html'
    private static final String WEB_RESOURCE_DIR = 'web'
    private static final String RESOURCE_RELATIVE_ROOT_INDEX = WEB_RESOURCE_DIR + File.separator + DEFAULT_INDEX

    private static final Map SOCKJS_CONF = [prefix: '/eventbus']

    private static final List SOCKJS_INBOUND_RULES = [
            [address: BusAddr.SEARCH.address],
            [address: BusAddr.SAVE_FILE_MAPPING.address]
    ]

    private static final List SOCKJS_OUTBOUND_RULES = [
            [address: BusAddr.SEARCH_RESULT.address]
    ]

    static final Integer HTTP_SEE_OTHER = 303
    static final Integer HTTP_BAD_REQUEST = 400
    static final Integer HTTP_NOT_FOUND = 404
    static final Integer HTTP_SERVER_ERROR = 500

    private static final List NG_ENTRY_POINTS = ['/']

    Map conf = [:]

    def start() {
        conf.host = container?.config?.host ?: '0.0.0.0'
        conf.port = container?.config?.port ?: 8081

        HttpServer server = vertx.createHttpServer()

        server.requestHandler(buildRouteMatcherClosure())

        vertx.createSockJSServer(server).bridge(SOCKJS_CONF, SOCKJS_INBOUND_RULES, SOCKJS_OUTBOUND_RULES)

        server.listen(conf.port, conf.host)

        container.logger.info("Gwez WebVerticle: http://${conf.host}:${conf.port}/")
    }

    private Closure buildRouteMatcherClosure() {
        RouteMatcher routeMatcher = new RouteMatcher()

        routeMatcher.put('/upload', this.&routeUpload)
        routeMatcher.get('/land/:sha1', this.&routeReassemble)
        routeMatcher.noMatch(this.&routeServeFile)

        routeMatcher.asClosure()
    }

    private def routeUpload(HttpServerRequest req) {

        String uploadedFilename = req.params.get('filename')
        container.logger.debug("Incoming upload: ${uploadedFilename}")
        if (uploadedFilename.contains('../')) {
            req.response.statusCode = HTTP_BAD_REQUEST
            req.response.end()
            return
        }

        req.pause()

        vertx.eventBus.send(BusAddr.GEN_BOARD_PASS.address, [:]) { boardingPass ->

            String boardingDir = boardingPass.body.directory
            String boardingName = boardingPass.body.filename

            String fileName = "${boardingDir}/${req.params.filename}"
            String tmpFileName = "${boardingDir}/${boardingName}"

            vertx.fileSystem.open(tmpFileName) { AsyncResult<AsyncFile> asyncRes ->
                if (asyncRes.succeeded) {
                    AsyncFile file = asyncRes.result
                    Pump pump = Pump.createPump(req, file)

                    req.endHandler {
                        file.close {
                            vertx.fileSystem.move(tmpFileName, fileName) {
                                vertx.eventBus.publish(MainVerticle.BUS_NAME + '.fileYard.onboardFile', [filename: fileName])
                            }
                        }
                    }

                    pump.start()
                    req.resume()
                    req.response.statusCode = 200
                    req.response.end()
                } else {
                    container.logger.error('Could not open file on upload', asyncRes.cause)
                    req.resume()
                    req.response.statusCode = HTTP_SERVER_ERROR
                    req.response.end()
                }
            }
        }
    }

    private def routeReassemble(HttpServerRequest req) {

        vertx.eventBus.send(BusAddr.GET_ASSEMBLY.address, [sha1: req.params['sha1']]) { allChunksResp ->
            vertx.eventBus.send(BusAddr.ASSEMBLE_FILE.address, allChunksResp.body) {
                container.logger.info("${allChunksResp.body.name} popped out of the warp")
                req.response.end()
            }
        }
    }

    private def routeServeFile(HttpServerRequest req) {
        if (NG_ENTRY_POINTS.contains(req.path)) {
            req.response.sendFile(RESOURCE_RELATIVE_ROOT_INDEX)
        } else {
            String resourceRelativePath = WEB_RESOURCE_DIR + req.path

            File targetFileOnDisk = findOnDisk(resourceRelativePath)

            if (!resourceRelativePath.contains('..') && targetFileOnDisk.exists()) {
                if (targetFileOnDisk.isDirectory()) {
                    Boolean endsWithSlash = req.path.endsWith('/')
                    req.response.headers['Location'] = req.path + (endsWithSlash ? '' : '/') + DEFAULT_INDEX
                    req.response.statusCode = HTTP_SEE_OTHER
                    req.response.end()
                } else {
                    req.response.sendFile(resourceRelativePath)
                }
            } else {
                req.response.statusCode = HTTP_NOT_FOUND
                req.response.end()
            }
        }
    }

    private File findOnDisk(String resourceRelativePath) {

        VertxInternal core = vertx.toJavaVertx() as VertxInternal
        String pathToDisk = PathAdjuster.adjust(core, resourceRelativePath)
        //println "${resourceRelativePath} is here :${pathToDisk} "

        new File(pathToDisk)
    }
}
