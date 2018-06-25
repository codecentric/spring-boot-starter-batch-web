File-to-db sample for spring-boot-starter-batch-web
=============================
This sample demonstrates the usage of an external database. 
To run it, start the main class org.hsqldb.server.Server, which is part of the dependencies, then run de.codecentric.batch.db.DatabaseInitializer to initialize the database. Now you can startup the Spring Boot application either by running the class de.codecentric.batch.Application directly in the IDE or by packaging everything up via mvn package, and then running it via java -jar xxx.jar.

The default port is 8080. To start a job, copy partner-import.csv to /tmp and then use this curl command:

$ curl --data 'jobParameters=pathToFile=file:/tmp/partner-import.csv' localhost:8080/batch/operations/jobs/flatfileJob