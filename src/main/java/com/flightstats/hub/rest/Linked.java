package com.flightstats.hub.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Linked<T> {

    private final HalLinks halLinks;
    private final T object;

    protected Linked(HalLinks halLinks, T object) {
        this.halLinks = halLinks;
        this.object = object;
    }

    @JsonProperty("_links")
    public HalLinks getHalLinks() {
        return halLinks;
    }

    @JsonProperty
    @JsonUnwrapped
    public T getObject() {
        return object;
    }

    public static <T> Builder<T> linked(T object) {
        return new Builder<>(object);
    }

    public static Builder<?> justLinks() {
        return linked(null);
    }

    public static class Builder<T> {
        private final List<HalLink> links = new ArrayList<>();
        private final Multimap<String, HalLink> multiLinks = Multimaps.newListMultimap(new HashMap<String, Collection<HalLink>>(),
                () -> new ArrayList<>());
        private final T object;

        public Builder(T object) {
            this.object = object;
        }

        public Builder<T> withLink(String name, URI uri) {
            links.add(new HalLink(name, uri));
            return this;
        }

        public Builder<T> withLink(String name, String uri) {
            links.add(new HalLink(name, URI.create(uri)));
            return this;
        }

        public Builder<T> withLinks(String name, List<HalLink> links) {
            multiLinks.putAll(name, links);
            return this;
        }

        public Linked<T> build() {
            return new Linked<>(new HalLinks(links, multiLinks), object);
        }
    }

    public void writeJson(ObjectNode node) {
        ObjectNode links = node.putObject("_links");
        for (HalLink link : halLinks.getLinks()) {
            ObjectNode linkNode = links.putObject(link.getName());
            linkNode.put("href", link.getUri().toString());
        }
    }
}
