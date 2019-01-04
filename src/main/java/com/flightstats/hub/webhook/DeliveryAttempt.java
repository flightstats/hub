package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentPath;
import com.sun.jersey.api.client.ClientResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data
@Builder
public class DeliveryAttempt {
    int number;
    Webhook webhook;
    ContentPath contentPath;
    String payload;
    @Setter
    Integer statusCode;
    @Setter
    Exception exception;
}
