package com.flightstats.hub.replication;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VerifierResult {

    private List<String> missingSequences = new ArrayList<>();
    private List<String> invalidPayloads = new ArrayList<>();
    private int sequencesChecked;
    private int payloadsChecked;

    public List<String> getMissingSequences() {
        return missingSequences;
    }

    public void addMissingSequence(String missingSequence) {
        this.missingSequences.add(missingSequence);
    }

    public List<String> getInvalidPayloads() {
        return invalidPayloads;
    }

    public void setInvalidPayloads(String missingPayload) {
        this.invalidPayloads.add(missingPayload);
    }

    public int getSequencesChecked() {
        return sequencesChecked;
    }

    public void incrementSequencesChecked() {
        this.sequencesChecked++;
    }

    public int getPayloadsChecked() {
        return payloadsChecked;
    }

    public void incrementPayloadsChecked() {
        this.payloadsChecked++;
    }

    @Override
    public String toString() {
        return "VerifierResult{" +
                "missingSequences=" + missingSequences +
                ", invalidPayloads=" + invalidPayloads +
                ", sequencesChecked=" + sequencesChecked +
                ", payloadsChecked=" + payloadsChecked +
                '}';
    }
}
