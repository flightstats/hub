package com.flightstats.hub.app;

import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.config.properties.PropertiesLoader;
import com.flightstats.hub.dao.aws.S3MaintenanceManager;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.util.SecretFilter;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;

import static com.flightstats.hub.constant.InternalResourceDescription.S3MM_DESCRIPTION;
import static com.flightstats.hub.constant.InternalResourceDescription.TIME_DESCRIPTION;

@Slf4j
@Path("/internal/s3Maintenance")
public class InternalS3MaintenanceManagerResource {

    private final S3MaintenanceManager s3MaintenanceManager;
    private final S3MaintenanceManager drS3MaintenanceManager;
    private final ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    public InternalS3MaintenanceManagerResource(@Named("MAIN") S3MaintenanceManager s3MaintenanceManager,
                                                @Named("DISASTER_RECOVERY") S3MaintenanceManager drS3MaintenanceManager,
                                                ObjectMapper objectMapper) {
        this.s3MaintenanceManager = s3MaintenanceManager;
        this.drS3MaintenanceManager = drS3MaintenanceManager;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getS3MaintenanceManagerResource() {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("description", S3MM_DESCRIPTION);
        root.put("details", "Introspect into the last enforcement of lifecycle rules");

        final ObjectNode links = root.putObject("_links");
        String uri = uriInfo.getRequestUri().toString();
        addLink(links, "self", uri);
        addLink(links, "main", uri + "/main");
        addLink(links, "dr", uri + "/dr");

        return Response.ok(root).build();
    }

    private void addLink(ObjectNode node, String key, String value) {
        final ObjectNode link = node.putObject(key);
        link.put("href", value);
    }

    @GET
    @Path("/main")
    public Response getMain() {
        return describe(s3MaintenanceManager.getLastLifecycleApplied());
    }

    @GET
    @Path("/dr")
    public Response getDr() {
        return describe(drS3MaintenanceManager.getLastLifecycleApplied());
    }

    private Response describe(S3MaintenanceManager.HubS3LifecycleRequest lifecycleRequest) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("lastRequestPresent", null != lifecycleRequest);
        if (lifecycleRequest == null) {
            return Response.ok(root).build();
        }

        root.put("applyDateTime", lifecycleRequest.getRequestTime().toString());
        root.put("bucket", lifecycleRequest.getRequest().getBucketName());
        ArrayNode rules = root.putArray("rules");
        for (BucketLifecycleConfiguration.Rule rule : lifecycleRequest.getRequest().getLifecycleConfiguration().getRules()) {
            ObjectNode aRule = rules.addObject();
            aRule.put("id", rule.getId());
            aRule.put("days", rule.getExpirationInDays());
            aRule.put("prefix", Optional.ofNullable(rule.getFilter())
                    .map(LifecycleFilter::getPredicate)
                    .filter(x -> x instanceof LifecyclePrefixPredicate)
                    .map(x -> (LifecyclePrefixPredicate) x)
                    .map(LifecyclePrefixPredicate::getPrefix)
                    .orElse(null));
        }

        return Response.ok(root).build();
    }
}