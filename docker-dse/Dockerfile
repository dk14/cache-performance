FROM ubuntu:14.04

#Packages

RUN apt-get -y install curl apt-transport-https

RUN echo "deb https://dmkndr%40gmail.com:TrustNo1@debian.datastax.com/enterprise stable main" | sudo tee -a /etc/apt/sources.list.d/datastax.sources.list &&\
    curl -L http://debian.datastax.com/debian/repo_key | sudo apt-key add -

RUN echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list &&\
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823

RUN apt-get update && apt-get -y install default-jdk supervisor git sbt dse-full

RUN sbt update

#clone tester
RUN mkdir /opt/tester &&\
    cd /opt/tester/ &&\
    git clone https://github.com/dk14/cache-performance.git

RUN cd /opt/tester/cache-performance && sbt test:package

COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY dse /etc/default/dse
COPY cassandra.yaml /etc/dse/cassandra/cassandra.yaml

EXPOSE 22

# http://docs.datastax.com/en/datastax_enterprise/4.8/datastax_enterprise/sec/secConfFirePort.html
EXPOSE 4040 7080 7081 8888 9160

#Solr port, inter-node
EXPOSE 8983 8984

#Cassandra inter-node,ssl, jmx
EXPOSE 7000 7001 7199

#Spark Master
EXPOSE 8984

#CQL
EXPOSE 9042

#HIVE
EXPOSE 10000

#OpsCenter
EXPOSE 50031 61620 61621

CMD ["/usr/bin/supervisord"]