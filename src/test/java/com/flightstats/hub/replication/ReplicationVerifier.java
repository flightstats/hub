package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.config.GuiceContext;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The replication verifier should get kicked off every N hours.
 * It should pull down the replication config for each source domain on system X.
 * For each replicated channel, it should verify that:
 *  - Since the last run, the sequence is continous
 *  - For a certain percentage, M, verify that the source payload matches the replicated payload
 */
public class ReplicationVerifier {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationVerifier.class);

    private static final Client client = GuiceContext.HubCommonModule.buildJerseyClient();
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            logger.warn("Usage: com.flightstats.hub.replication.ReplicationVerifier replicationDomain frequencyHours verificationPercent");
            logger.warn("replicationDomain is the full uri of where replication is - http://hub.svc.staging/");
            logger.warn("frequencyHours is how far back in time this process should look for data - 1");
            logger.warn("verificationPercent is what percentage of the payloads from the replication source should be verified as matching.  This will put for more on the source system. 0-100");
        }
        String replicationUri = args[0];
        int frequencyHours = Integer.parseInt(args[1]);
        int verificationPercent = Integer.parseInt(args[2]);
        ChannelUtils channelUtils = new ChannelUtils(GuiceContext.HubCommonModule.buildJerseyClientNoRedirects(), client);
        SequenceFinder sequenceFinder = new SequenceFinder(channelUtils);
        ClientResponse replication = client.resource(replicationUri + "replication").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        if (replication.getStatus() != 200) {
            logger.warn("invalid response for " + replicationUri + " " + replication);
            return;
        }
        JsonNode replicationStatus = mapper.readTree(replication.getEntityInputStream());

        JsonNode status = replicationStatus.get("status");
        ExecutorService executor = Executors.newFixedThreadPool(status.size());
        Collection<ChannelVerifier> verifiers = new ArrayList<>();
        for (JsonNode stati : status) {
            logger.info(stati.toString());
            verifiers.add(new ChannelVerifier(stati, replicationUri, frequencyHours, verificationPercent, sequenceFinder, channelUtils));

        }
        List<Future<VerifierResult>> futures = executor.invokeAll(verifiers);
        for (Future<VerifierResult> future : futures) {
            logger.info(future.get().toString());
        }
        executor.shutdown();
    }
}
