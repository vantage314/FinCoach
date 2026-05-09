FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mkdir -p /root/.m2 && printf '%s\n' \
  '<?xml version="1.0" encoding="UTF-8"?>' \
  '<toolchains>' \
  '  <toolchain>' \
  '    <type>jdk</type>' \
  '    <provides>' \
  '      <version>21</version>' \
  '      <vendor>any</vendor>' \
  '    </provides>' \
  '    <configuration>' \
  '      <jdkHome>/opt/java/openjdk</jdkHome>' \
  '    </configuration>' \
  '  </toolchain>' \
  '</toolchains>' \
  > /root/.m2/toolchains.xml
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/core-0.0.1-SNAPSHOT.jar app.jar
COPY render-entrypoint.sh /app/render-entrypoint.sh

ENV SPRING_PROFILES_ACTIVE=prod-pg
EXPOSE 8080

ENTRYPOINT ["sh", "/app/render-entrypoint.sh"]
