package com.flightstats.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.model.HealthStatus;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FilenameFilter;

@Path("/health")
public class HealthResource {
    private final static Logger logger = LoggerFactory.getLogger(HealthResource.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static String version;

    @Inject
    HubHealthCheck healthCheck;

    @Inject
    @Named("app.lib_path")
    String libPath;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
        HealthStatus healthStatus = healthCheck.getStatus();
        rootNode.put("healthy", healthStatus.isHealthy());
        rootNode.put("description", healthStatus.getDescription());
        rootNode.put("version", getVersion());
        rootNode.put("startTime", HubMain.getStartTime().toString());

        if (healthStatus.isHealthy()) {
            return Response.ok(rootNode).build();
        } else {
            return Response.serverError().entity(rootNode).build();
        }
    }


    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        try {
            File libDir = new File(libPath);
            File[] files = libDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("hub");
                }
            });
            if (files.length == 1) {
                String name = files[0].getName();
                version = StringUtils.removeEnd(StringUtils.removeStart(name, "hub-"), ".jar");
            } else if (files.length == 0) {
                version = "no hub jar file found";
            } else {
                String fileNames = "";
                for (File file : files) {
                    fileNames += file.getName() + ";";
                }
                version = "multiple hub jar files found: " + fileNames;
            }
        } catch (Exception e) {
            logger.info("unable to get version ", e);
            version = "unknown";
        }
        return version;
    }
}
