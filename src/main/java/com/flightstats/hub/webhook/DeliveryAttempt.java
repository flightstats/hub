package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentPath;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data
@Builder
class DeliveryAttempt {
    int number;
    Webhook webhook;
    ContentPath contentPath;
    String payload;
    @Setter
    Integer statusCode;
    @Setter
    Exception exception;
}
