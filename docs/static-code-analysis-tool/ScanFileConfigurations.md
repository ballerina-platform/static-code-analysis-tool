# Scan File Configurations

# Contents

- [Overview](#overview)
- [Configuration for Platform Plugins](#configuration-for-platform-plugins)
- [Configuration for Static Code Analyzer Plugins](#configuration-for-static-code-analyzer-plugins)
- [Configuration for Static Code Analyzer Rules](#configuration-for-static-code-analyzer-rules)
- [Configuration for Suppressions](#configuration-for-suppressions)
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

Each setting must be a TOML boolean; a value such as `"true"` stops the scan with an error. A suppression that does not meet a requirement fails the scan before any issue is printed, saved or sent to a platform, and the error gives the suppression's location. The command-line options can disable suppressions or switch a requirement on for a single run, but cannot undo the `Scan.toml` settings.
