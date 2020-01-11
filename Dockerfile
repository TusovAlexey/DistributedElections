FROM zookeeper
RUN /apache-zookeeper-3.5.6-bin/bin/zkCli.sh deleteall /CA
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["zkServer.sh", "start=foreground"]

