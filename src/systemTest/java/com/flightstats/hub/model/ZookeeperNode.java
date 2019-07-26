package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Builder
@Value
public class ZookeeperNode {
    Links _links;
    ZookeeperNodeData data;

    @Builder.Default
    List<ZookeeperChildNode> children = new ArrayList<>();
}
