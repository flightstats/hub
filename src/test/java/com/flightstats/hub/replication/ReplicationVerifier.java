package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.config.GuiceContext;
import com.sun.jersey.api.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The replication verifier should get kicked off every N hours.
 * It should pull down the replication config for each source domain on system X.
 * For each replicated channel, it should verify that:
 *  - The sequence is continous for the last N hours
 *  - For a certain percentage, M, verify that the source payload matches the replicated payload
 *
 *  //todo - gfm - 12/22/14 - replace this with javascript
 */
public class ReplicationVerifier {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationVerifier.class);

    private static final Client client = GuiceContext.HubCommonModule.buildJerseyClient();
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        /*if (args.length != 3) {
            logger.warn("Usage: com.flightstats.hub.replication.ReplicationVerifier replicationDomain frequencyHours verificationPercent");
            logger.warn("replicationDomain is the full uri of where replication is - http://hub.svc.staging/");
            logger.warn("frequencyHours is how far back in time this process should look for data - 1");
            logger.warn("payloadPercent is what percentage of the payloads from the replication source should be verified as matching.  This will put for more on the source system. 0-100");
        }
        String replicationUri = args[0];
        int frequencyHours = Integer.parseInt(args[1]);
        int payloadPercent = Integer.parseInt(args[2]);
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
            verifiers.add(new ChannelVerifier(stati, replicationUri, frequencyHours, payloadPercent, sequenceFinder, channelUtils));

        }
        int missing = 0;
        int sequences = 0;
        int payloads = 0;
        List<Future<VerifierResult>> futures = executor.invokeAll(verifiers);
        for (Future<VerifierResult> future : futures) {
            VerifierResult result = future.get();
            logger.info(result.toString());
            missing += result.getMissingSequences().size();
            missing += result.getInvalidPayloads().size();
            payloads += result.getPayloadsChecked();
            sequences += result.getSequencesChecked();
        }
        logger.info("total sequences checked " + sequences);
        logger.info("total payloads checked " + payloads);
        logger.info("returning missing count " + missing);
        executor.shutdown();

        System.exit(missing);*/
    }
}
