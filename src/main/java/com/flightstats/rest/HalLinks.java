package com.flightstats.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HalLinks {

    private final List<HalLink> links = new ArrayList<HalLink>();

    public HalLinks(List<HalLink> links) {
        this.links.addAll(links);
    }

    public List<HalLink> getLinks() {
        return Collections.unmodifiableList(links);
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
        return links.equals(halLinks.links);
    }

    @Override
    public int hashCode() {
        return links.hashCode();
    }

    @Override
    public String toString() {
        return "HalLinks{" +
                "links=" + links +
                '}';
    }
}
