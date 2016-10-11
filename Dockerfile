#vim: ft=dockerfile

FROM ubuntu:16.04

ADD . /srv/traychallenge
WORKDIR /srv/traychallenge
EXPOSE 9000

RUN apt-get update && apt-get install -y scala apt-transport-https

# http://www.scala-sbt.org/0.13/docs/Installing-sbt-on-Linux.html
RUN bash -c 'echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list'
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN apt-get update
RUN apt-get install -y sbt

RUN sbt compile

CMD sbt run
