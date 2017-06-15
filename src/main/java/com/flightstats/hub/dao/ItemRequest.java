package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;
import lombok.*;
import lombok.experimental.Wither;

import java.net.URI;
import java.util.Date;

@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"channel", "key"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemRequest {
    @Wither
    private final String channel;
    private final String tag;
    private final URI uri;
    private final ContentKey key;
    private Date date = new Date();
    private boolean remoteOnly = false;

}
