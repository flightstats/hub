package com.flightstats.hub.rest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HalLinks {

    private final List<HalLink> links = new ArrayList<>();
    private final Multimap<String, HalLink> multiLinks = ArrayListMultimap.create();

    public HalLinks(List<HalLink> links) {
        this(links, ArrayListMultimap.create());
    }

    public HalLinks(List<HalLink> links, Multimap<String, HalLink> multiLinks) {
        this.links.addAll(links);
        this.multiLinks.putAll(multiLinks);
    }

    public List<HalLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public Multimap<String, HalLink> getMultiLinks() {
        return Multimaps.unmodifiableMultimap(multiLinks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HalLinks halLinks = (HalLinks) o;

        if (!links.equals(halLinks.links)) {
            return false;
        }
        return multiLinks.equals(halLinks.multiLinks);
    }

    @Override
    public int hashCode() {
        int result = links.hashCode();
        result = 31 * result + multiLinks.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "HalLinks{" +
                "links=" + links +
                '}';
    }
}
