package org.eu.galaxie.vertx.mod.gwez.verticles

import org.eu.galaxie.vertx.mod.gwez.BusAddr
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import java.security.MessageDigest

class FileYardVerticle extends Verticle {

    private static final String SHA = 'SHA-256'
    private static final String TMP_FILE_SUFFIX = '.uploading'
    private static final Integer READ_BUFFER_SIZE = 8 * 1024
    private static final Integer FILE_CHUNKS_SIZE = 512 * 1024

    private static Map conf = [:]

    def start() {

        readContainerConf()

        [
                (BusAddr.GEN_BOARD_PASS.address): this.&getBoardingPass,
                (BusAddr.ONBOARD_FILE.address)  : this.&onboardFile,
                (BusAddr.ASSEMBLE_FILE.address) : this.&landFile,
                (BusAddr.SHA256_SUM.address)    : this.&calculateSha1
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

    private void getBoardingPass(Message message) {
        message.reply([
                directory: conf.boardingDir,
                filename : (UUID.randomUUID().toString() + TMP_FILE_SUFFIX)
        ])
    }

    private void onboardFile(Message message) {

        String filePathToOnboard = message.body.filename

        def content = splitFile(filePathToOnboard, FILE_CHUNKS_SIZE)

        vertx.eventBus.publish(BusAddr.SAVE_FILE_MAPPING.address, [name: filePathToOnboard.split('/').last(), sha1: content.sha1])

        int nbChunks = content.chunks.size()

        content.chunks.eachWithIndex { chunkSha1, index ->
            vertx.eventBus.publish(BusAddr.SAVE_FILE_MAPPING.address, [
                    sha1     : chunkSha1,
                    belongsTo: content.sha1,
                    num      : index + 1,
                    total    : nbChunks
            ])
        }
    }

    private Map splitFile(String fileToSplit, Integer chunkSize) {

        MessageDigest digestOriginal = MessageDigest.getInstance(SHA)
        MessageDigest digestCurrentChunk = MessageDigest.getInstance(SHA)

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

    private void calculateSha1(Message message) {

        MessageDigest digest = MessageDigest.getInstance(SHA)

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

    static String digestToString(MessageDigest digest) {
        new BigInteger(1, digest.digest()).toString(16).padLeft(40, '0')
    }
}
