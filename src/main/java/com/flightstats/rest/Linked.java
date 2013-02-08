package com.flightstats.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Linked<T> {

    private final HalLinks links;
    private final T object;

    private Linked(HalLinks links, T object) {
        this.links = links;
        this.object = object;
    }

    public HalLinks getLinks() {
        return links;
    }

    public T getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Linked linked = (Linked) o;

        if (links != null ? !links.equals(linked.links) : linked.links != null) {
            return false;
        }
        if (object != null ? !object.equals(linked.object) : linked.object != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = links != null ? links.hashCode() : 0;
        result = 31 * result + (object != null ? object.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Linked{" +
                "links=" + links +
                ", object=" + object +
                '}';
    }

    public static <T> Builder<T> linked(T object){
        return new Builder<T>(object);
    }

    public static class Builder<T> {
        private final List<HalLink> links = new ArrayList<>();
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

        public Linked<T> build(){
            return new Linked<>(new HalLinks(links), object);
        }
    }
}
