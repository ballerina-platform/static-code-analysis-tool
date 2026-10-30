# Scan File Configurations

# Contents

- [Overview](#overview)
- [Configuration for Platform Plugins](#configuration-for-platform-plugins)
- [Configuration for Static Code Analyzer Plugins](#configuration-for-static-code-analyzer-plugins)
- [Configuration for Static Code Analyzer Rules](#configuration-for-static-code-analyzer-rules)
- [Configuration for Suppressions](#configuration-for-suppressions)
- [Configuration for the Gate](#configuration-for-the-gate)
- [References](#references)

# Overview

A `Scan.toml` file can be created to configure the behavior of the Ballerina static code analysis tool. The configuration file is picked if there’s one in the working directory.

```text
📦ballerina_project
 ┣ 📜.devcontainer.json
 ┣ 📜.gitignore
 ┣ 📜Ballerina.toml
 ┣ 📜main.bal
 ┗ 📜Scan.toml
```

The path to the configuration file can also be specified in the `Ballerina.toml` file as follows:

```toml
[scan]
configPath = "path/to/Scan.toml"
```

# Configuration for Platform Plugins

Users can configure platform plugins in a `Scan.toml` file by specifying the plugin JAR path to report analysis results.

```toml
[[platform]]
name = "sonarqube"
path = "path/to/sonar_platform_plugin.jar"
```

Both `name` and `path` are required. A platform entry without a `path` is ignored. The `path` can be a local file path (resolved relative to the current working directory) or a URL, in which case the scan tool downloads the plugin JAR.

```toml
[[platform]]
name = "sonarqube"
path = "https://example.com/sonar_platform_plugin.jar"
```

Users can also provide additional arguments to the platform plugin by specifying them in the `Scan.toml` file.

```toml
[[platform]]
name = "sonarqube"
path = "path/to/sonar_platform_plugin.jar"
sonarProjectPropertiesPath = "sonar-project.properties"
```

# Configuration for Static Code Analyzer Plugins

Users can configure static code analyzer plugins in a `Scan.toml` file. These plugins are automatically added as imports by a code generator during scans, ensuring they are only engaged during a `bal scan`.

```toml
[[analyzer]]
org = "org1"
name = "name1"
```

For locally available static code analyzer plugins, the following configuration can be used:

```toml
[[analyzer]]
org = "org2"
name = "name2"
version = "version"
repository = "local"
```

# Configuration for Static Code Analyzer Rules

Users can specify the rules to filter out specific issues during an analysis by defining them in a `Scan.toml` file.

Define the rules to include in the analysis:

```toml
[rule]
include = ["ballerina:101", "ballerina/io:101"]
```

Define the rules to exclude in the analysis:

```toml
[rule]
exclude = ["ballerina:101", "ballerina/io:101"]
```

# Configuration for Suppressions

Issues can be accepted in the source with the `@scan:Suppress` annotation from the `ballerina/scan` package. Users can control how suppressions are applied by defining the `[suppression]` table in a `Scan.toml` file:

```toml
[suppression]
enabled = true
requireReason = true
requireRuleId = true
```

- `enabled` - Apply `@scan:Suppress` annotations. Defaults to `true`. When `false`, every issue is reported as though no suppression existed, as `--ignore-suppressions` does for a single run; this suits a shared `Scan.toml` that must always show the unsuppressed results.
- `requireReason` - Fail the scan when a suppression does not give a reason. Defaults to `false`. Same as `--require-suppression-reason`. Has no effect with `--ignore-suppressions` or `enabled = false`.
- `requireRuleId` - Fail the scan when a suppression does not name the rules it suppresses. Defaults to `false`. Same as `--require-suppression-rule-id`. Has no effect with `--ignore-suppressions` or `enabled = false`.

The settings must be written as a `[suppression]` table. Each setting must be a TOML boolean, and the table accepts no other keys; a value such as `"true"` or an unknown key such as `requireReasons` stops the scan with an error, since it would otherwise leave a requirement off without notice. A suppression that does not meet a requirement fails the scan before any issue is printed, saved or sent to a platform, and the error gives the suppression's location. The command-line options can disable suppressions or switch a requirement on for a single run, but cannot undo the `Scan.toml` settings.

# Configuration for the Gate

The gate fails the scan, with exit code `1`, when the number of issues at or above a severity reaches a count. Users can set it by defining the `[gate]` table in a `Scan.toml` file:

```toml
[gate]
failOnSeverity = "MEDIUM"        # BLOCKER | HIGH | MEDIUM | LOW | INFO | NONE
failOnCount = 5                  # 1 or more
```

- `failOnSeverity` - Count the issues at or above this severity. The value is case-insensitive, and `NONE` turns the gate off. Same as `--fail-on-severity`.
- `failOnCount` - Fail when this many counted issues are found. A count of `5` fails on the fifth issue and allows four. Same as `--fail-on-count`.

With neither setting, there is no gate and findings never fail the scan. A count without a severity counts every severity, and a severity without a count fails on the first issue at or above it. The gate counts the issues left after rule selection and `@scan:Suppress` annotations, and a rule without a severity counts by its kind: `BUG` as `HIGH`, `VULNERABILITY` as `MEDIUM` and `CODE_SMELL` as `LOW`. The gate only changes the exit code; the console output, reports and platforms always receive every issue.

Each setting is resolved on its own, and a command-line option overrides the same setting in `Scan.toml` with a warning when the values differ. For example, `--fail-on-severity=NONE` turns off a gate set in `Scan.toml`, and `--fail-on-count` turns on a gate that `failOnSeverity = "NONE"` turns off, counting every severity. An invalid value or an unknown key in the `[gate]` table stops the scan with an error that gives its line and column.

In a workspace, a `Scan.toml` at the workspace root can hold a `[gate]` table, which sets one gate for the issues of all packages together. Any other section in the workspace `Scan.toml` is ignored with a warning. When the command line or the workspace `Scan.toml` sets a gate, the `[gate]` tables of the packages are ignored with a warning. Otherwise each package is checked against the gate in its own `Scan.toml`.
