# ============================================================
# Build Stage
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy parent pom first
COPY pom.xml .

# Copy module pom files first for better Docker layer caching
COPY common-library/pom.xml common-library/pom.xml
COPY employee-service/pom.xml employee-service/pom.xml
COPY department-service/pom.xml department-service/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY payroll-service/pom.xml payroll-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY kafka-module/pom.xml kafka-module/pom.xml
COPY leave-service/pom.xml leave-service/pom.xml

# Copy source code
COPY common-library/src common-library/src
COPY employee-service/src employee-service/src
COPY department-service/src department-service/src
COPY eureka-server/src eureka-server/src
COPY api-gateway/src api-gateway/src
COPY payroll-service/src payroll-service/src
COPY auth-service/src auth-service/src
COPY kafka-module/src kafka-module/src
COPY leave-service/src leave-service/src

# Service to build
ARG SERVICE_NAME

# Build selected service and required internal modules
RUN mvn -pl ${SERVICE_NAME} -am clean package -DskipTests


# ============================================================
# Runtime Stage
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

ARG SERVICE_NAME

COPY --from=builder /workspace/${SERVICE_NAME}/target/${SERVICE_NAME}-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]