package com.flightstats.hub.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;

import java.util.List;

public class WebhookStatus {
    private ContentPath lastCompleted;
    private ContentKey channelLatest;
    private Webhook webhook;
    private List<String> errors;
    private List<ContentPath> inFlight;

    @java.beans.ConstructorProperties({"lastCompleted", "channelLatest", "webhook", "errors", "inFlight"})
    WebhookStatus(ContentPath lastCompleted, ContentKey channelLatest, Webhook webhook, List<String> errors, List<ContentPath> inFlight) {
        this.lastCompleted = lastCompleted;
        this.channelLatest = channelLatest;
        this.webhook = webhook;
        this.errors = errors;
        this.inFlight = inFlight;
    }

    public static WebhookStatusBuilder builder() {
        return new WebhookStatusBuilder();
    }

    @JsonIgnore
    public Webhook getWebhook() {
        return webhook;
    }

    public String getName() {
        return webhook.getName();
    }

    public ContentPath getLastCompleted() {
        return this.lastCompleted;
    }

    public ContentKey getChannelLatest() {
        return this.channelLatest;
    }

    public List<String> getErrors() {
        return this.errors;
    }

    public List<ContentPath> getInFlight() {
        return this.inFlight;
    }

    public static class WebhookStatusBuilder {
        private ContentPath lastCompleted;
        private ContentKey channelLatest;
        private Webhook webhook;
        private List<String> errors;
        private List<ContentPath> inFlight;

        WebhookStatusBuilder() {
        }

        public WebhookStatus.WebhookStatusBuilder lastCompleted(ContentPath lastCompleted) {
            this.lastCompleted = lastCompleted;
            return this;
        }

        public WebhookStatus.WebhookStatusBuilder channelLatest(ContentKey channelLatest) {
            this.channelLatest = channelLatest;
            return this;
        }

        public WebhookStatus.WebhookStatusBuilder webhook(Webhook webhook) {
            this.webhook = webhook;
            return this;
        }

        public WebhookStatus.WebhookStatusBuilder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public WebhookStatus.WebhookStatusBuilder inFlight(List<ContentPath> inFlight) {
            this.inFlight = inFlight;
            return this;
        }

        public WebhookStatus build() {
            return new WebhookStatus(lastCompleted, channelLatest, webhook, errors, inFlight);
        }

        public String toString() {
            return "com.flightstats.hub.webhook.WebhookStatus.WebhookStatusBuilder(lastCompleted=" + this.lastCompleted + ", channelLatest=" + this.channelLatest + ", webhook=" + this.webhook + ", errors=" + this.errors + ", inFlight=" + this.inFlight + ")";
        }
    }
}
