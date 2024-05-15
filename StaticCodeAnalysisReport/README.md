# Ballerina Static Code Analysis Report

## Overview

This module consists of the React code required to build the static code analysis report for the scan tool.

## Prerequisites

1. OpenJDK 17 ([Adopt OpenJDK](https://adoptium.net/temurin/releases/?version=17) or any other OpenJDK distribution)

2. [Node.js (version v20.12.0)](https://nodejs.org/en/blog/release/v20.12.0)

3. [npm (version 10.5.0 or later)](https://www.npmjs.com/package/npm) 

## Get started

1. Install the required project dependencies by running the following command.

   ```bash
   npm install
   ```

2. Start the application.

   ```bash
   npm run dev
   ```

3. To build and update the scan tool with a new static analysis report, execute the following command.

   ```bash
   ./gradlew createScanReportZip
   ```
   
> **Note**: The scan tool tests related to the analysis report will fail after the build process as the generate JS and CSS files in the zip have changed. Consider updating the test resource outputs after a build.