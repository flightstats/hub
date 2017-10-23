echo create tag webhook
HOST="http://hub.pdx.dev.flightstats.io"
# HOST="http://hub.iad.dev.flightstats.io"
# HOST="http://localhost:9080"
http PUT $HOST/webhook/twh  \
tagUrl=$HOST/tag/twh \
callbackUrl=http://nothing/callback

read -rsp $'Press any key to continue creating 2 channels ...\n' -n1 key

echo create 2 tagged channels
http PUT $HOST/channel/twhc1 ttlDays=8 description:='"a channel for testing"' \
tags:='["twh"]'
http PUT $HOST/channel/twhc2 ttlDays=8 description:='"a channel for testing"' \
tags:='["twh"]'


sleep 1

echo see if automatically created wh instances show up
http --verbose GET $HOST/webhook/TAGWH_twh_twhc1
http --verbose GET $HOST/webhook/TAGWH_twh_twhc2

read -rsp $'Press any key to continue cleanup...\n' -n1 key

echo remove tag webhook
http DELETE $HOST/webhook/twh

sleep 5

echo see if  related webhooks also got deleted
http $HOST/webhook/TAGWH_twh_twhc1
http $HOST/webhook/TAGWH_twh_twhc2


http DELETE $HOST/channel/twhc1
http DELETE $HOST/channel/twhc2
http DELETE $HOST/webhook/TAGWH_twh_twhc1
http DELETE $HOST/webhook/TAGWH_twh_twhc2
