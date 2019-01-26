package com.flightstats.hub.webhook.error;

import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

@Builder
@Value
public class WebhookError {
    String name;
    DateTime creationTime;
    String data;
}
