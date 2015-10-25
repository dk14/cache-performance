FROM ubuntu:14.04

RUN apt-get update && apt-get install -y openssh-server

RUN mkdir /var/run/sshd
RUN echo 'root:screencast' | chpasswd
RUN sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config

RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]

RUN sudo apt-get -y install default-jdk

RUN apt-get -y install apt-transport-https
RUN echo "deb https://dmkndr%40gmail.com:TrustNo1@debian.datastax.com/enterprise stable main" | sudo tee -a /etc/apt/sources.list.d/datastax.sources.list
RUN apt-get -y install curl
RUN curl -L http://debian.datastax.com/debian/repo_key | sudo apt-key add -
RUN sudo apt-get update

RUN sudo apt-get -y install dse-full

