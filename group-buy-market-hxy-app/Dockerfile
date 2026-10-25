# 已在 Windows/WSL 完成 Maven 打包时使用的快速构建入口。
ARG JAVA_BASE_IMAGE=eclipse-temurin:8-jre
FROM ${JAVA_BASE_IMAGE}
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY target/group-buy-market-hxy-app.jar /app/app.jar
EXPOSE 8091
# JAVA_TOOL_OPTIONS 由 JVM 读取，避免旧脚本把 JVM 参数放在 -jar 后面。
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
