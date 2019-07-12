package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class ZookeeperNode {
    Links _links;
    ZookeeperNodeData data;
    List<ZookeeperChildNode> children;
    /*
    {
"_links": {
"self": {
"href": "http://hub.iad.staging.flightstats.io/internal/zookeeper/ChannelLatestUpdated/3ScaleAccountReport",
"description": "Read-only interface into the ZooKeeper hierarchy."
},
"depth": {
"href": "http://hub.iad.staging.flightstats.io/internal/zookeeper/ChannelLatestUpdated/3ScaleAccountReport?depth=2",
"description": "Use depth=2 to see child counts two levels deep."
},
"olderThanDays": {
"href": "http://hub.iad.staging.flightstats.io/internal/zookeeper/ChannelLatestUpdated/3ScaleAccountReport?olderThanDays=14",
"description": "Use olderThanDays to limit the results to leaf nodes modfied more than olderThanDays ago."
}
},
"data": {
"bytes": "MTk3MC8wMS8wMS8wMC8wMC8wMC8wMDEvbm9uZQ==",
"string": "1970/01/01/00/00/00/001/none",
"long": 3546926861620622000
},
"stats": {
"created": "2018-04-05T21:39:36.619Z",
"modified": "2019-06-03T20:56:31.946Z",
"changes": 33437
},
"children": []
}
     */
}
