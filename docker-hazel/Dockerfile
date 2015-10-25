FROM ubuntu:14.04

RUN apt-get update && apt-get install -y openssh-server

RUN mkdir /var/run/sshd
RUN echo 'root:screencast' | chpasswd
RUN sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config

RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile

RUN apt-get -y install default-jdk
RUN apt-get -y install supervisor

RUN mkdir /opt/hazel && wget -O /opt/hazel/hazelcast.jar http://central.maven.org/maven2/com/hazelcast/hazelcast-all/3.5.3/hazelcast-all-3.5.3.jar

COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
EXPOSE 22 5701
CMD ["/usr/bin/supervisord"]
