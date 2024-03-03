# Ballerina Static Code Analysis Tool

## Overview
Static Code Analysis (SCA) is an approach that uses tools to examine code without execution. 
It's used for identifying potential issues like bugs, vulnerabilities and style violations.
SCA improves software quality by early issue detection, creating better maintainability, and
providing enhanced security. The Ballerina scan tool provides a set of command line tools to
staticaly analyze Ballerina files and report analysis issues.

## Prerequisites
1. openJDK 17 ([Adopt OpenJDK](https://adoptium.net/temurin/releases/?version=17) or any other OpenJDK distribution)

2. [Ballerina version: 2201.8.5](https://ballerina.io/downloads/)


## Building from the source
Execute the commands below to build from the source.

1. Export Github Personal access token with read package permissions as follows,
    ```bash
    export packageUser=<Username>
    export packagePAT=<Personal access token>
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

## Usage
1. Open a Ballerina project

2. Check if the `scan` command is added by running
    ```bash
    bal tool list
    ```

3. Perform static code analysis on the project by running
    ```bash
    bal scan
    ```

## Features
1. Run analysis against all Ballerina documents in the current package
    ```bash
    bal scan
    ```

2.  Run analysis against a standalone Ballerina file. The file path can be relative or absolute.
    ```bash
    bal scan main.bal
    ```

3. Run analysis and save analysis results in specified directory.
    ```bash
    bal scan --target-dir="results"
    ```

## Contribute to Ballerina
As an open-source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct
All the contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.