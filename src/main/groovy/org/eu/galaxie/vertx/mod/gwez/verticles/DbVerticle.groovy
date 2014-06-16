package org.eu.galaxie.vertx.mod.gwez.verticles

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

// TODO: get random 20 nodes
// TODO: store queries

class DbVerticle extends Verticle {

    static final String PATH = '/tmp/gwez.orient.db'

    ODatabaseDocumentTx db

    def start() {

        initBase()

        [
                'org.eu.galaxie.vertx.mod.gwez.db.getBySha1': this.&getBySha1,
                'org.eu.galaxie.vertx.mod.gwez.db.create': this.&create,
                'org.eu.galaxie.vertx.mod.gwez.db.create.node': this.&createNode,
                'org.eu.galaxie.vertx.mod.gwez.db.search': this.&search,
                'org.eu.galaxie.vertx.mod.gwez.db.getChunkMap': this.&getChunkMap
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
    }

    private void initBase() {
        db = new ODatabaseDocumentTx("local:${PATH}")

        if (!new File(PATH).exists()) {
            db.create()
        } else {
            db.open('admin', 'admin')
        }
    }


    private void create(Message message) {
// TODO: gérer les doublons de sha1
        def newDoc = new ODocument(message.body)
        println "Saving to file"
        newDoc.setClassName('File')
        db.begin()
        newDoc.save()
        db.commit()
    }

    void createNode(Message message) {
        // TODO: gérer les doublons de domain
        println "Creating node: ${message.body}"
        def newDoc = new ODocument(message.body)
        newDoc.setClassName('Node')
        db.begin()
        newDoc.save()
        db.commit()

        println "Nodes: " + queryAndCollect('select * from Node', ['domain']).join(', ')
    }

    private void getBySha1(Message message) {
        def fieldList = ['name', 'sha1']
        def query = "select ${fieldList.join(',')} from File where sha1 = '${message.body.sha1}'"
        message.reply([hits: queryAndCollect(query, fieldList)])
    }

    private void search(Message message) {
        def fieldList = ['name', 'sha1']
        def query = "select ${fieldList.join(',')} from File where name like '%${message.body.query}%'"
        println query
        message.reply([hits: queryAndCollect(query, fieldList)])
    }

    private void getChunkMap(Message message) {
        def fieldList = ['total', 'num', 'sha1']
        def query = "select ${fieldList.join(',')} from File where belongsTo = '${message.body.sha1}'"
        message.reply([hits: queryAndCollect(query, fieldList)])
    }

    private List queryAndCollect(String query, List<String> fields) {

        List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query))
        if (!result) {
            return []
        }
        result.collect { ODocument document ->
            fields.collectEntries { String fieldName ->
                [(fieldName): document.field(fieldName)]

            }
        }
    }
}
