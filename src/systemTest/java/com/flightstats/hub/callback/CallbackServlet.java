package com.flightstats.hub.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.callback.model.RequestObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CallbackServlet extends HttpServlet {

    private final Map<String, List<String>> cacheRequestObject;
    private ObjectMapper objectMapper;

    public CallbackServlet() {
        super();
        this.cacheRequestObject = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        ;
    }

    @SneakyThrows
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        parseRequestBody(request.getReader());
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @SneakyThrows
    private void parseRequestBody(BufferedReader reader) {
        RequestObject requestObject = objectMapper.readValue(reader, RequestObject.class);
        String webhookName = requestObject.getName();

        if (cacheRequestObject.containsKey(webhookName)) {
            cacheRequestObject.get(webhookName).addAll(requestObject.getUris());
        } else {
            cacheRequestObject.put(requestObject.getName(), new ArrayList<>(requestObject.getUris()));
        }
    }

    @SneakyThrows
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String webhookName = request.getParameter("webhookname");

        if (cacheRequestObject.containsKey(webhookName)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(cacheRequestObject.get(webhookName));
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
