FROM hub.cdqidi.cn/tomcat:8.5-alpine-utf8
LABEL MAINTAINER="flash520@163.com"
# 拷贝执行文件
COPY ./target/sagacity-docs/. /usr/local/tomcat/webapps/ROOT/

# 安装自定义中文字体
COPY ./src/main/webapp/asset/fonts/simsun.ttc /usr/share/fonts/simsun.ttc
# 后端生成图片包含中文需要
RUN apk add --update ttf-dejavu fontconfig && rm -rf /var/cache/apk/*
# 运行 Tomcat
WORKDIR /usr/local/tomcat/
EXPOSE 80
# CMD java -Xms2048m -Xmx2048m -jar gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod