# Scan Tool Overview

Static Code Analysis (SCA) uses tools to examine code without executing the code. They are used for identifying potential issues like bugs, vulnerabilities, and style violations. SCA improves software quality by detecting issues early, ensuring better maintainability, and providing enhanced security. Ballerina supports SCA using the Ballerina scan tool.

The scan tool compile and perform static code analysis, print results to the console, and report results. It analyzes the source code defined in each module when compiling a package or analyzes the given source file when compiling a single Ballerina file. 

Note: Analyzing individual Ballerina files of a package is not allowed.

## Synopsis

```bash
bal scan [OPTIONS] [<package>|<source-file>]
```

## CLI Options

- Specify target path for saving analysis reports. (Only for ballerina build projects)

```text
--target-dir=<path>
```
-  Generate an HTML report containing the analysis results. (Only for ballerina build projects)

```text
--scan-report
```

-  List all available rules

```text
--list-rules
```

-  Run analysis for a specific set of rules.

```text
--include-rules=<rule1, ...>
```

-  Exclude analysis for a specific set of rules.

```text
--exclude-rules=<rule1, ...>
```

- Define platform(s) to report results. The user can define more than one platform.

```text
--platforms=<platformName1, ...>
```

## Examples

- Run analysis against all Ballerina documents in the current package, print results to the console, and save results in JSON file format in the target directory.

```bash
bal scan
```

- Run analysis against a standalone Ballerina file and print results to the console. The file path of the Ballerina file can be relative or absolute.

```bash
bal scan main.bal
```

- Run analysis and save analysis results in a specified directory.

```bash
bal scan --target-dir="results"
```

- Run analysis and generate an HTML report in the target directory.

```bash
bal scan --scan-report
```

- View all available rules.

```bash
bal scan --list-rules
```

- Run analysis for a specific rule.

```bash
bal scan --include-rules="ballerina:101"
```

- Run analysis for a specific set of rules.

```bash
bal scan --include-rules="ballerina:101, ballerina/io:101"
```

- Exclude analysis for a specific rule.

```bash
bal scan --exclude-rules="ballerina/io:101"
```

- Exclude analysis for a specific set of rules.

```bash
bal scan --exclude-rules="ballerina:101, ballerina/io:101"
```

- Run analysis and report to sonarqube

```bash
bal scan --platforms=sonarqube
```

- Run analysis and report to multiple platforms

```bash
bal scan --platforms="sonarqube, semgrep, codeql"
```