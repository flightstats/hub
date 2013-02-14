package com.flightstats.datahub.model;

import java.util.UUID;

public class ValueInsertedResponse {

    private final UUID id;

    public ValueInsertedResponse(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
