<!--

    Copyright (c) 2020-present Sonatype, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# Sonatype Scan Gradle Plugin #
[![Maven Central](https://img.shields.io/maven-central/v/org.sonatype.gradle.plugins/scan-gradle-plugin.svg?label=Maven%20Central)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.sonatype.gradle.plugins%22%20AND%20a%3A%22scan-gradle-plugin%22)

[![Gradle Plugins Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/sonatype/gradle/plugins/scan/org.sonatype.gradle.plugins.scan.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=Gradle%20Plugins%20Portal)](https://plugins.gradle.org/plugin/org.sonatype.gradle.plugins.scan)

[![CircleCI Build Status](https://circleci.com/gh/sonatype-nexus-community/scan-gradle-plugin.svg?style=shield "CircleCI Build Status")](https://circleci.com/gh/sonatype-nexus-community/scan-gradle-plugin) 

Gradle plugin that scans the dependencies of a Gradle project using Sonatype platforms: OSS Index and Nexus IQ Server.

## Compile and Publish to Local Maven Cache

> ./gradlew clean publishToMavenLocal

If you want to save some time, skip integration tests:

> ./gradlew clean publishToMavenLocal -x integrationTest

## Run Integration Tests

> ./gradlew integrationTest

## Compatibility
The plugin can be used on projects with Gradle 3.0 or higher (local installation or wrapper) and Java 8 installed locally.

## Supported Programming Languages
Gradle can be used to build projects developed in various programming languages. This plugin supports:
- Java
- Kotlin
- Scala
- Groovy

## How to Use
- Create/Clone/Download any Gradle project.
- Edit its `build.gradle` file adding this:
```
plugins {
  id 'org.sonatype.gradle.plugins.scan' version '1.0.3' // Update the version as needed
}
```

### OSS Index
OSS Index can be used without any extra configuration, but to avoid reaching the limit for anonymous queries every user
is encouraged to create a free account on [OSS Index](https://ossindex.sonatype.org/user/signin) and use the credentials
on this plugin. Cache can also be configured optionally.
```
ossIndexAudit {
    username = 'email' // if not provided, an anonymous query will be made
    password = 'pass'
    useCache = true // true by default
    cacheDirectory = 'some/path' // by default it uses the user data directory (according to OS)
    cacheExpiration = 'PT12H' // 12 hours if omitted. It must follow the Joda Time specification at https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-
}
```
- Open Terminal on the project's root and run `./gradlew ossIndexAudit`
- You should see the audit result on Terminal.

### Nexus IQ Server
- Start a local instance of IQ Server, or get the URL and credentials of a remote one.
- Configure IQ Server settings inside the `nexusIQScan` configuration on the file `build.gradle` e.g.
```
nexusIQScan {
    username = 'admin' // Make sure to use an user with the role 'Application Evaluator' in the given IQ Server application
    password = 'pass'
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
    stage = 'build' // build is used if omitted
}
```
- Open Terminal on the project's root and run `./gradlew nexusIQScan`
- You should see the scan report URL report on Terminal.

### Sensitive Data
Sometimes it's not desirable to keep sensitive data stored on `build.gradle`. For such cases it's possible to use project
properties (-P arguments) or environment variables (-D arguments or injected from a tool) from command line when running
the `nexusIQScan` or `ossIndexAudit` tasks.

Here is an example using project properties for the credentials:

```
nexusIQScan {
    username = project['username']
    password = project['password']
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
}

ossIndexAudit {
    username = project['username']
    password = project['password']
}
```

On command line:
```
./gradlew nexusIQScan -Pusername=admin -Ppassword=pass
```

```
./gradlew ossIndexAudit -Pusername=admin -Ppassword=pass
```

Each property name can be set as needed.

Here is an example using environment variables for the credentials:

```
nexusIQScan {
    username = System.getenv('username')
    password = System.getenv('password')
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
}

ossIndexAudit {
    username = System.getenv('username')
    password = System.getenv('password')
}
```

As mentioned above the values can be set on command line using -D arguments or injected via a tool (CI/CD for instance).

### Multi-module projects
Just apply the plugin on the root project and all sub-modules will be processed and the output will be a single report
with all components found in each module. This includes Android projects.

## Contributing

We care a lot about making the world a safer place, and that's why we created this `scan-gradle-plugin`. If you as well want to speed up the pace of software development by working on this project, jump on in! Before you start work, create a new issue, or comment on an existing issue, to let others know you are!

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of ours
to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do NOT file Sonatype support tickets related to `scan-gradle-plugin` support in regard to this project
* DO file issues here on GitHub, so that the community can pitch in

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using `scan-gradle-plugin`, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
