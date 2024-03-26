# Ballerina Static Code Analysis Tool

## Overview

Static Code Analysis (SCA) uses tools to examine code without executing the code. They are used for identifying potential issues like bugs, vulnerabilities, and style violations. SCA improves software quality by detecting issues early, ensuring better maintainability, and providing enhanced security. Ballerina supports SCA using the Ballerina scan tool. The Ballerina scan tool provides the command-line functionality to statically analyze Ballerina files and report analysis results.

This repository consists of

- The Ballerina scan tool implementation.
- The core scan logic.
- The extension points for introducing additional analysis and reporting results to static code analysis platforms.

## Prerequisites

1. OpenJDK 17 ([Adopt OpenJDK](https://adoptium.net/temurin/releases/?version=17) or any other OpenJDK distribution)

2. [Ballerina](https://ballerina.io/)

## Building from the source

Execute the commands below to build from the source.

1. Export GitHub Personal access token with read package permissions as follows,

    ```bash
    export packageUser=<GitHub username>
    export packagePAT=<GitHub personal access token>
    ```

2. To build the package:

    ```bash
    ./gradlew clean build
    ```
   
> **Note**: The scan tool configurations will be appended to the contents of the `.ballerina/.config/bal-tools.toml` file during the build process.

3. To run the tests:

    ```bash
    ./gradlew clean test
    ```

4. To build the package without tests:

    ```bash
    ./gradlew clean build -x test
    ```

## Contribute to Ballerina

As an open-source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All the contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.