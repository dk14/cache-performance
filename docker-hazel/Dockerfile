FROM ubuntu:14.04

RUN apt-get update && apt-get install -y openssh-server

#SSH
RUN mkdir /var/run/sshd
RUN echo 'root:screencast' | chpasswd
RUN sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config

RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile

#Packages
RUN apt-get -y install supervisor

RUN apt-get -y install apt-transport-https

RUN apt-get -y install default-jdk

RUN echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list &&\
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823 &&\
    apt-get update &&\
    apt-get -y install sbt 

RUN sbt update

RUN apt-get -y install git

RUN mkdir /opt/hazel && wget -O /opt/hazel/hazelcast.jar http://central.maven.org/maven2/com/hazelcast/hazelcast-all/3.5.3/hazelcast-all-3.5.3.jar

#clone tester
RUN mkdir /opt/tester &&\
    cd /opt/tester/ &&\
    git clone https://github.com/dk14/cache-performance.git
RUN cd /opt/tester/cache-performance && sbt test:package

#configure
COPY hazelcast.xml /etc/hazelcast.xml
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
EXPOSE 22 5701
CMD ["/usr/bin/supervisord"]
