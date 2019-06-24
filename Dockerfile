FROM gradle:jdk8 as builder

RUN mkdir -p /home/gradle/blather
COPY --chown=gradle:gradle build.gradle /home/gradle/blather
COPY --chown=gradle:gradle src/ /home/gradle/blather/src
COPY --chown=gradle:gradle settings.gradle /home/gradle/blather

WORKDIR /home/gradle/blather
RUN gradle build

FROM openjdk:8-jdk-alpine
MAINTAINER Don Stryker <don.stryker@yahoo.com>
COPY --from=builder /home/gradle/blather/build/libs/blather.jar /app/app.jar
WORKDIR /app
CMD ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Xmx128m -Xss256k -jar app.jar" ]


