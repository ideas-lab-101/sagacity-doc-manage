FROM hub.cdqidi.cn/tomcat:8.5-alpine-utf8
LABEL MAINTAINER="flash520@163.com"
# 更新镜像作者信息
COPY ./target/sagacity-docs/. /usr/local/tomcat/webapps/ROOT/
WORKDIR /data/
EXPOSE 8080
# CMD java -Xms2048m -Xmx2048m -jar gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod