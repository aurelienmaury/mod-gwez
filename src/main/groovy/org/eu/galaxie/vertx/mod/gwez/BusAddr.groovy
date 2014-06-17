package org.eu.galaxie.vertx.mod.gwez

enum BusAddr {

    SEARCH('search'),
    SEARCH_RESULT('search.result'),
    SAVE_FILE_MAPPING('db.save.file-mapping'),
    SEARCH_FILE_BY_NAME('db.search.by-name'),
    GET_CHUNKS_MAP('db.get.chunks'),
    GET_FILE_MAPPING_BY_SHA1('db.get.by-sha1'),
    GEN_BOARD_PASS('file.boarding-pass'),
    ONBOARD_FILE('file.onboard'),
    ASSEMBLE_FILE('file.assemble'),
    GET_ASSEMBLY('get.assembly'),
    SHA256_SUM('file.sha256.sum')

    private static final String BUS_NAME = 'org.eu.galaxie.vertx.mod.gwez'

    private String address


    private BusAddr(String suffix) {
        this.address = BUS_NAME + '.' + suffix
    }

    String getAddress() {
        this.address
    }
}
