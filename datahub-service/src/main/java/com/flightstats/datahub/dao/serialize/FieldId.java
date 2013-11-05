package com.flightstats.datahub.dao.serialize;

public enum FieldId {

    //TODO: Put a notice in here about the sensitivity of these numeric values and that these fields must never be deleted?
    CONTENT_TYPE(1),
    CONTENT_LANGUAGE(2),
    CONTENT(3),
    MILLIS(4);

    private final int id;

    FieldId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static FieldId fromInt(int val){
        for (FieldId fieldId : values()) {
            if(fieldId.getId() == val){
                return fieldId;
            }
        }
        throw new IllegalArgumentException("No such Field with id = " + val);
    }
}
