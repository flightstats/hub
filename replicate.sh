#!/bin/sh

curl -i -X PUT --header "Content-type: application/json" --data '{ "historicalDays" : 10, "excludeExcept": ["testy1", "testy2", "testy3", "testy4", "testy5", "testy6", "testy7", "testy8", "testy9", "testy10"] }' http://localhost:8080/replication/hub.svc.dev

#curl -i -X DELETE http://localhost:8080/replication/dev.null
