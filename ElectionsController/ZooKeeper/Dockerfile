FROM socrata/java8
WORKDIR /zookeeper
ADD zoo.cfg .
ADD start.sh .
RUN curl -O http://www-eu.apache.org/dist/zookeeper/zookeeper-3.4.14/zookeeper-3.4.14.tar.gz \
        && tar xvf zookeeper-3.4.14.tar.gz \
        && chmod +x start.sh && mkdir -p /data/zookeeper
CMD ./start.sh