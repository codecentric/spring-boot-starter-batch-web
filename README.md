Enterprise-ready production-ready batch applications powered by Spring Boot
=============================
[![Build Status](https://travis-ci.org/codecentric/spring-boot-starter-batch-web.png?branch=master)](https://travis-ci.org/codecentric/spring-boot-starter-batch-web)
[![Coverage Status](https://coveralls.io/repos/codecentric/spring-boot-starter-batch-web/badge.svg?branch=master&service=github)](https://coveralls.io/github/codecentric/spring-boot-starter-batch-web?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.codecentric/spring-boot-starter-batch-web/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.codecentric/spring-boot-starter-batch-web/)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


The project spring-boot-starter-batch-web is a Spring Boot starter for Spring Batch taking care of everything except writing the jobs.

See the [Documentation](http://codecentric.github.io/spring-boot-starter-batch-web/) for detailed infos, examples and operational details.

Features include:

* Starting up a web application and automatically deploying batch jobs to it (JavaConfig, XML or JSR-352).
* Log file separation, one log file for each job execution.
* An operations http endpoint for starting and stopping jobs, for retrieving the BatchStatus and the log file.
* A monitoring http endpoint for retrieving detailed information on a job execution, for knowing all deployed jobs and all running job executions.

Take a look at the [Getting Started page](http://codecentric.github.io/spring-boot-starter-batch-web/#_getting_started).

There are the following samples available:

[batch-boot-simple](https://github.com/codecentric/spring-samples/tree/master/batch-boot-simple): a very simple JavaConfig sample with an embedded database.

[batch-boot-file-to-db](https://github.com/codecentric/spring-samples/tree/master/batch-boot-file-to-db): a job configured in xml using job parameters that reads from a file and writes to a database. This sample demonstrates the usage of an external database.

[batch-boot-simple-jsr352](https://github.com/codecentric/spring-samples/tree/master/batch-boot-simple-jsr352): job samples in JSR-352 style.
