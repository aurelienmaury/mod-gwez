package org.eu.galaxie.vertx.mod.gwez.verticles

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import java.security.MessageDigest

class FileYardVerticle extends Verticle {

    private static final String SHA1 = 'SHA1'

    private static final Integer READ_BUFFER_SIZE = 8 * 1024

    private static Map conf = [:]

    def start() {
        readContainerConf()
        [
                'org.eu.galaxie.vertx.mod.gwez.fileYard.getBoardingPass': this.&getBoardingPass,
                'org.eu.galaxie.vertx.mod.gwez.fileYard.onboardFile': this.&onboardFile,
                'org.eu.galaxie.vertx.mod.gwez.fileYard.landFile': this.&landFile,
                'org.eu.galaxie.vertx.mod.gwez.fileYard.calculateSha1': this.&calculateSha1
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
    }

    private void readContainerConf() {
        conf.boardingDir = container?.config?.boardingDir ?: System.getProperty("user.dir") + '/work/boarding'
        conf.warpDir = container?.config?.warpDir ?: System.getProperty("user.dir") + '/work/warp'
        conf.landingDir = container?.config?.landingDir ?: System.getProperty("user.dir") + '/work/landing'

        ensureDirectoryExists(conf.boardingDir)
        ensureDirectoryExists(conf.warpDir)
        ensureDirectoryExists(conf.landingDir)
    }

    private void ensureDirectoryExists(String path) {
        def dirFile = new File(path)
        if (dirFile.exists()) {
            if (dirFile.isFile()) {
                throw new IllegalStateException("${path} should be a directory")
            }
        } else {
            container.logger.debug("Attempting to create directory: ${path}")
            if (!dirFile.mkdirs()) {
                throw new IllegalStateException("${path} impossible to create")
            }
        }
    }

    private Map splitFile(String fileToSplit, Integer chunkSize) {

        MessageDigest digestOriginal = MessageDigest.getInstance(SHA1)
        MessageDigest digestCurrentChunk = MessageDigest.getInstance(SHA1)

        def orderedParts = []
        def chunk = new File("${conf.warpDir}/chunk")
        def chunkCompletion = 0

        new File(fileToSplit).eachByte(READ_BUFFER_SIZE) { byte[] buf, int bytesRead ->

            digestOriginal.update(buf, 0, bytesRead)
            digestCurrentChunk.update(buf, 0, bytesRead)

            if (bytesRead < READ_BUFFER_SIZE) {
                buf = buf[0..(bytesRead - 1)]
            }

            chunk.append(buf)

            chunkCompletion += bytesRead

            boolean desiredChunkComplete = chunkCompletion >= chunkSize || bytesRead < READ_BUFFER_SIZE

            if (desiredChunkComplete) {
                String chunkSha = digestToString(digestCurrentChunk)
                chunk.renameTo("${conf.warpDir}/${chunkSha}")
                orderedParts << chunkSha
                chunk = new File("${conf.warpDir}/chunk")
                chunkCompletion = 0
                digestCurrentChunk.reset()
            }
        }

        chunk.delete()

        [sha1: digestToString(digestOriginal), chunks: orderedParts]
    }


    private void getBoardingPass(Message message) {
        message.reply([
                directory: conf.boardingDir,
                filename: "${UUID.randomUUID()}.uploading" as String
        ])
    }

    private void onboardFile(Message message) {
        println "onboarding ${message.body.filename}"
        message.reply(splitFile(message.body.filename, 512 * 1024))
    }

    private void calculateSha1(Message message) {

        MessageDigest digest = MessageDigest.getInstance('SHA1')

        new File(message.body.filename).eachByte(READ_BUFFER_SIZE) { byte[] buf, int bytesRead ->
            digest.update(buf, 0, bytesRead)
        }

        message.reply([success: true, sha1: digestToString(digest)])
    }

    private void landFile(Message message) {

        String targetFile = message.body.name
        String targetSha1 = message.body.sha1

        def newComer = new File("${conf.landingDir}/${targetFile}")

        if (newComer.exists()) {
            newComer.delete()
        }

        newComer.withOutputStream { out ->

            message.body.chunks.each { chunkName ->
                println "adding ${chunkName}"
                new File("${conf.warpDir}/${chunkName}").eachByte(READ_BUFFER_SIZE) { byte[] buf, int bytesRead ->
                    out.write(buf, 0, bytesRead)
                }
            }
        }

        message.reply([success: true])
    }

    private String digestToString(MessageDigest digest) {
        new BigInteger(1, digest.digest()).toString(16).padLeft(40, '0')
    }
}
