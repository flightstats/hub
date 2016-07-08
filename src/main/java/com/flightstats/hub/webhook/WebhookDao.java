package com.flightstats.hub.webhook;

import com.google.common.base.Optional;

import java.util.Collection;

public interface WebhookDao {
    Webhook upsert(Webhook webhook);

    Optional<Webhook> get(String name);

    Collection<Webhook> getAll();

    void delete(String name);
}
