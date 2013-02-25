package com.flightstats.datahub.service;

public enum CustomHttpHeaders {

    CREATION_DATE_HEADER("Creation-Date");

    private final String headerName;

    CustomHttpHeaders(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }
}
