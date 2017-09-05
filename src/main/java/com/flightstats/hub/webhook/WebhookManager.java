package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.flightstats.hub.app.HubServices.register;

@Singleton
public class WebhookManager {

    private final static Logger logger = LoggerFactory.getLogger(WebhookManager.class);

    private static final String WATCHER_PATH = "/groupCallback/watcher";

    @Inject
    private WatchManager watchManager;
    @Inject
    @Named("Webhook")
    private Dao<Webhook> webhookDao;
    @Inject
    private Provider<WebhookLeader> v2Provider;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private ActiveWebhooks activeWebhooks;

    @Inject
    @Named("HubCluster")
    private Cluster hubCluster;

    @Inject
    private WebhookError webhookError;
    @Inject
    private WebhookContentPathSet webhookInProcess;

    private Map<String, WebhookLeader> localLeaders = new HashMap<>();
    private final Client client = RestClient.createClient(5, 15, true, true);

    @Inject
    public WebhookManager() {
        register(new WebhookIdleService(), HubServices.TYPE.AFTER_HEALTHY_START, HubServices.TYPE.PRE_STOP);
    }

    private void start() {
        logger.info("starting");
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                manageWebhooks();
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }

        });
        manageWebhooks();
    }

    private synchronized void manageWebhooks() {
        //todo - gfm - do we really need to run this N(odes) times for all webhooks?
        /*
        //todo - gfm -

        call /run on every item in Dao & ZK
            running server is responsible for checking if the webhook has changed
        if is in ZK and not in Dynamo, stop it on the running server
        is key is orphaned in ZK, delete the key
         */

        Set<String> v1Webhooks = activeWebhooks.getV1();
        Set<String> v2Webhooks = activeWebhooks.getV2();
        Set<Webhook> daoWebhooks = new HashSet<>(webhookDao.getAll(false));

        for (Webhook daoWebhook : daoWebhooks) {
            if (v1Webhooks.contains(daoWebhook.getName())) {
                //if is in v1 ZK, leave it alone ...
                //todo - gfm - this can go away, eventually
                logger.info("found v1 webhook {}", daoWebhook.getName());
            } else if (v2Webhooks.contains(daoWebhook.getName())) {
                logger.debug("found existing v2 webhook {}", daoWebhook.getName());
                //todo - gfm - let the existing server evaluate if the webhook has changed
                //todo - gfm - call /run at existing server
            } else {
                logger.debug("found new v2 webhook {}", daoWebhook.getName());

                List<String> servers = new ArrayList<>(hubCluster.getAllServers());
                Collections.shuffle(servers);
                for (String server : servers) {
                    String url = HubHost.getScheme() + server + "/internal/webhook/run/" + daoWebhook.getName();
                    logger.info("calling {}", url);
                    ClientResponse response = client.resource(url).put(ClientResponse.class);
                    if (response.getStatus() != 200) {
                        logger.warn("unexpected response {}", response);
                    } else {
                        logger.debug("success {}", response);
                        break;
                    }
                }
            }
        }

        Set<String> daoNames = daoWebhooks.stream()
                .map(Webhook::getName)
                .collect(Collectors.toSet());
        v2Webhooks.removeAll(daoNames);
        for (String orphanedV2 : v2Webhooks) {
            //todo - gfm - call stop on the orphan
        }
    }

    void runRemote(String name) {
        //todo - gfm - get server name from zookeeper, if it is running ..

    }

    boolean runLocal(String name) {
        logger.info("run {}", name);
        if (localLeaders.containsKey(name)) {
            logger.info("checking for change {}", name);
            //todo - gfm - if it is running, check if it has changed

            return false;
        } else {
            logger.info("starting new {}", name);
            Webhook webhook = webhookDao.get(name);
            WebhookLeader webhookLeader = v2Provider.get();
            boolean hasLeadership = webhookLeader.tryLeadership(webhook);
            if (hasLeadership) {
                localLeaders.put(webhook.getName(), webhookLeader);
            }
            return hasLeadership;
        }
    }

    //todo - gfm - it would be nice if notify for a single change did not trigger calls about all webhooks
    void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public void delete(String name) {
        //todo - gfm - call delete on the server running the webhook ...

    }

    public void getStatus(Webhook webhook, WebhookStatus.WebhookStatusBuilder statusBuilder) {
        statusBuilder.lastCompleted(lastContentPath.get(webhook.getName(), WebhookStrategy.createContentPath(webhook), WebhookLeader.WEBHOOK_LAST_COMPLETED));
        try {
            statusBuilder.errors(webhookError.get(webhook.getName()));
            ArrayList<ContentPath> inFlight = new ArrayList<>(new TreeSet<>(webhookInProcess.getSet(webhook.getName(), WebhookStrategy.createContentPath(webhook))));
            statusBuilder.inFlight(inFlight);
        } catch (Exception e) {
            logger.warn("unable to get status " + webhook.getName(), e);
            statusBuilder.errors(Collections.emptyList());
            statusBuilder.inFlight(Collections.emptyList());
        }
    }

    private class WebhookIdleService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            //todo - gfm - stop any webhooks on this server
        }

    }
}
