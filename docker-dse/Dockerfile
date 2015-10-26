FROM ubuntu:14.04

#SSH
RUN apt-get update && apt-get install -y openssh-server

RUN mkdir /var/run/sshd
RUN echo 'root:screencast' | chpasswd
RUN sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config

RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile

#Packages

RUN apt-get -y install default-jdk

RUN apt-get -y install apt-transport-https &&\
    echo "deb https://dmkndr%40gmail.com:TrustNo1@debian.datastax.com/enterprise stable main" | sudo tee -a /etc/apt/sources.list.d/datastax.sources.list &&\
    apt-get -y install curl &&\
    curl -L http://debian.datastax.com/debian/repo_key | sudo apt-key add - &&\
    sudo apt-get update &&\
    sudo apt-get -y install dse-full

RUN apt-get -y install supervisor

RUN apt-get -y install apt-transport-https
RUN echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list &&\
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823 &&\
    apt-get update &&\
    apt-get -y install sbt 

RUN sbt update

COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY dse /etc/default/dse
COPY cassandra.yaml /etc/dse/cassandra/cassandra.yaml
EXPOSE 22

# http://docs.datastax.com/en/datastax_enterprise/4.8/datastax_enterprise/sec/secConfFirePort.html
EXPOSE 4040,7080,7081,8888,9160

#Solr port, inter-node
EXPOSE 8983,8984

#Cassandra inter-node, jmx
EXPOSE 7000,7199

#Spark Master
EXPOSE 8984

#CQL
EXPOSE 9042

#HIVE
EXPOSE 10000

#OpsCenter
EXPOSE 50031,61620,61621

CMD ["/usr/bin/supervisord"]

