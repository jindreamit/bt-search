# 多阶段构建：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 创建Maven配置目录并配置阿里云镜像
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>*</mirrorOf><name>Aliyun Maven</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制pom.xml并下载依赖（利用Docker缓存和阿里云镜像）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（跳过测试以加快构建速度）
RUN mvn clean package -DskipTests -B -s /root/.m2/settings.xml

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 创建非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# 从构建阶段复制jar包
COPY --from=builder /app/target/bt-search.jar app.jar

# JVM参数优化
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
