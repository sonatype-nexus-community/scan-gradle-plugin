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
# Nexus IQ Server Gradle Plugin #
[![Maven Central](https://img.shields.io/maven-central/v/org.sonatype.gradle.plugins/scan-gradle-plugin.svg?label=Maven%20Central)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.sonatype.gradle.plugins%22%20AND%20a%3A%22scan-gradle-plugin%22)

[![CircleCI](https://circleci.com/gh/sonatype-nexus-community/scan-gradle-plugin.svg?style=svg)](https://circleci.com/gh/sonatype-nexus-community/scan-gradle-plugin) 

Gradle plugin that scans the dependencies of a Gradle project using Nexus IQ Server.

## Compile and Publish to Local Maven Cache

> ./gradlew clean publishToMavenLocal

If you want to save some time, skip integration tests:

> ./gradlew clean publishToMavenLocal -x integrationTest

## Run Integration Tests

> ./gradlew integrationTest

## Compatibility
The plugin can be used on projects with Gradle 4.2.1 or higher (local installation or wrapper) and Java 8 installed locally.

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
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'org.sonatype.gradle.plugins:scan-gradle-plugin:0.2.0' // Update the version as needed
    }
}

apply plugin: 'org.sonatype.gradle.plugins.scan'
```
- Start a local instance of IQ Server, or get the URL and credentials of a remote one.
- Configure IQ Server settings inside the `nexusIQScan` configuration on the file `build.gradle` e.g.
```
nexusIQScan {
    username = 'admin'
    password = 'pass'
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
    stage = 'build' // build is used if omitted
}
```
- Open Terminal on the project's root and run `./gradlew nexusIQScan --info`
- You should see the scan report URL report on Terminal.

### Sensitive Data
Sometimes it's not desirable to keep sensitive data stored on `build.gradle`. For such cases it's possible to use project
properties (-P arguments) or environment variables (-D arguments) from command line when running the `nexusIQScan` task.
Here is an example using project properties for the credentials:

```
nexusIQScan {
    username = project['username']
    password = project['password']
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
}
```

On command line: `./gradlew nexusIQScan -Pusername=admin -Ppassword=pass --info`

Each property name can be set as needed.

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
