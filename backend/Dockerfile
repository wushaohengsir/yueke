FROM eclipse-temurin:17-jre
WORKDIR /app
# 构建时由 docker build 上下文 backend/ 提供（jar 由 mvn package 产出）
COPY backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
