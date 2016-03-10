FROM haproxy:1.5

COPY haproxy.cfg /usr/local/etc/haproxy/haproxy.cfg

RUN mkdir /run/haproxy

CMD /usr/local/sbin/haproxy -f /usr/local/etc/haproxy/haproxy.cfg
