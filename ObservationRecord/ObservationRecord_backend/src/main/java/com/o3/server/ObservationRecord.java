package com.o3.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ObservationRecord {
    
    private String recordIdentifier;
    private String recordDescription;
    private String recordPayload;
    private String recordRightAscension;
    private String recordDeclination;
    private String recordOwner;
    public ZonedDateTime sent;
    private String Observatory = null;
    private String observatoryWeather = null;
    //Implement get and set functions for the variables

    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }

    public String getRecordDescription() {
        return recordDescription;
    }

    public void setRecordDescription(String recordDescription) {
        this.recordDescription = recordDescription;
    }

    public String getRecordPayload() {
        return recordPayload;
    }

    public void setRecordPayload(String recordPayload) {
        this.recordPayload = recordPayload;
    }

    public String getRecordRightAscension() {
        return recordRightAscension;
    }

    public void setRecordRightAscension(String recordRightAscension) {
        this.recordRightAscension = recordRightAscension;
    }

    public String getRecordOwner() {
        return recordOwner;
    }

    public void setRecordOwner(String recordOwner) {
        this.recordOwner = recordOwner;
    }

    public String getRecordDeclination() {
        return recordDeclination;
    }

    public void setRecordDeclination(String recordDeclination) {
        this.recordDeclination = recordDeclination;
    }

    public String getObservatory() {
        return Observatory;
    }

    public void setObservatory(String Observatory) {
        this.Observatory = Observatory;
    }

    public String getObservatoryWeather() {
        return observatoryWeather;
    }

    public void setObservatoryWeather(String observatoryWeather) {
        this.observatoryWeather = observatoryWeather;
    }

    public ObservationRecord(String recordIdentifier, String recordDescription, String recordPayload, String recordRightAscension, String recordDeclination,String recordOwner, String sent, String Observatory, String observatoryWeather) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;
        this.recordOwner = recordOwner;
        this.sent = ZonedDateTime.parse(sent);
        this.Observatory = Observatory;
        this.observatoryWeather = observatoryWeather;
    }

    public ObservationRecord(String recordIdentifier, String recordDescription, String recordPayload, String recordRightAscension, String recordDeclination,String recordOwner, String sent, String Observatory) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;
        this.recordOwner = recordOwner;
        this.sent = ZonedDateTime.parse(sent);
        this.Observatory = Observatory;
    }

    public ObservationRecord(String recordIdentifier, String recordDescription, String recordPayload, String recordRightAscension, String recordDeclination,String recordOwner, String Observatory) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;
        this.recordOwner = recordOwner;
        this.Observatory = Observatory;
    }


    /*By providing these help functions, you can convert time to needed formats easily, consider using similar
    auxiliary functions in other classes as well */

     long dateAsInt() {
     return sent.toInstant().toEpochMilli();
     }
     void setSent(long epoch) {
     sent = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
     }
}
