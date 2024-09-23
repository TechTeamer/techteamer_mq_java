# techteamer_mq_java

A RabbitMQ wrapper for java written in Kotlin

> To run tests fill out the proper config in src/test/kotlin/TestHelper so the client will be able to connect properly.

# Building a fat jar

In an openjdk:11 container volume the entire project, then from its root run `./gradlew build -x test -x dependencyCheckAnalyze`.
 - Excluding test for the build because it needs a rabbit container to run
 - Excluding the dependency check because it is to be done separately before release; it can freeze up the build
