FROM ubuntu:17.04

MAINTAINER "Ryan Whaley" whaleyr@pharmgkb.org

WORKDIR /app

ADD . /app

# update
RUN apt-get -y update
RUN apt-get -y install graphviz

CMD ["dot", "-Tpng", "diagram.dot"]
