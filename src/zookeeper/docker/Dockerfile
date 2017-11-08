FROM flightstats/base:openjdk-8jre-alpine

# Heavily influenced from:
# https://github.com/31z4/zookeeper-docker/blob/master/3.4.10/Dockerfile

RUN apk add --no-cache \
    bash \
    su-exec

ENV ZOO_USER=zookeeper \
    ZOO_UID=2000 \
    ZOO_CONF_DIR=/conf \
    ZOO_DATA_DIR=/data \
    ZOO_DATA_LOG_DIR=/datalog \
    ZOO_PORT=2181 \
    ZOO_VERSION=3.5.3-beta

# Add a user and make dirs
RUN set -ex; \
    adduser -D "$ZOO_USER" -u $ZOO_UID; \
    mkdir -p "$ZOO_DATA_LOG_DIR" "$ZOO_DATA_DIR" "$ZOO_CONF_DIR"; \
    chown "$ZOO_USER:$ZOO_USER" "$ZOO_DATA_LOG_DIR" "$ZOO_DATA_DIR" "$ZOO_CONF_DIR"

#ARG GPG_KEY=C823E3E5B12AF29C67F81976F5CECB3CB5E9BD2D
ARG DISTRO_NAME=zookeeper-"$ZOO_VERSION"

# Download Apache Zookeeper, verify its PGP signature, untar and clean up
RUN set -ex; \
    apk add --no-cache --virtual .build-deps \
        gnupg; \
    wget -q "http://www.apache.org/dist/zookeeper/$DISTRO_NAME/$DISTRO_NAME.tar.gz"; \
    wget -q "http://www.apache.org/dist/zookeeper/$DISTRO_NAME/$DISTRO_NAME.tar.gz.asc"; \
    # GPG Verification is disabled as the file is actually just a tar archive.
    # export GNUPGHOME="$(mktemp -d)"; \
    # gpg --keyserver ha.pool.sks-keyservers.net --recv-key "$GPG_KEY" || \
    # gpg --keyserver pgp.mit.edu --recv-keys "$GPG_KEY" || \
    # gpg --keyserver keyserver.pgp.com --recv-keys "$GPG_KEY"; \
    # gpg --batch --verify "$DISTRO_NAME.tar.gz.asc" "$DISTRO_NAME.tar.gz"; \
    tar -xf "$DISTRO_NAME.tar.gz"; \
    rm -rf "$GNUPGHOME" "$DISTRO_NAME.tar.gz" "$DISTRO_NAME.tar.gz.asc"; \
    apk del .build-deps

WORKDIR $DISTRO_NAME
VOLUME ["$ZOO_CONF_DIR", "$ZOO_DATA_DIR", "$ZOO_DATA_LOG_DIR"]

EXPOSE $ZOO_PORT 2888 3888

ENV PATH=$PATH:/$DISTRO_NAME/bin \
    ZOOCFGDIR=$ZOO_CONF_DIR

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["zkServer.sh", "start-foreground"]
