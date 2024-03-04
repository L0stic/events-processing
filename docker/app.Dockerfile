FROM gradle:7.6-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble --no-daemon


# Running stage
FROM openjdk:17-alpine
EXPOSE 5002
RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/chain-events-processing-*.jar /app/chain-events-processing.jar

# Labels to allow ELK analyze logs with json decode processor
LABEL   log_json="allow" \
        log_type="spring"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=10.0"

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /app/chain-events-processing.jar" ]
