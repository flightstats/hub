package com.flightstats.hub.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WebhookStatus {
    private ContentPath lastCompleted;
    private ContentKey channelLatest;
    private Webhook webhook;
    private List<String> errors;
    private List<ContentPath> inFlight;

    @JsonIgnore
    public Webhook getWebhook() {
        return webhook;
    }

    public String getName() {
        return webhook.getName();
    }

}
