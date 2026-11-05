# Scan File Configurations

# Contents

- [Overview](#overview)
- [Configuration for Platform Plugins](#configuration-for-platform-plugins)
- [Configuration for Static Code Analyzer Plugins](#configuration-for-static-code-analyzer-plugins)
- [Configuration for Static Code Analyzer Rules](#configuration-for-static-code-analyzer-rules)
- [Configuration for Suppressions](#configuration-for-suppressions)
- [Configuration for the Gate](#configuration-for-the-gate)
- [Configuration for Test Sources](#configuration-for-test-sources)
- [Configuration for Workspaces](#configuration-for-workspaces)
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

Both `name` and `path` are required. A platform entry without a `path` is ignored. The `path` can be a local file path, resolved relative to the folder of the `Scan.toml` file, or a URL, in which case the scan tool downloads the plugin JAR. The paths in a remote `Scan.toml` file are resolved relative to the package root.

A declared platform is reported to only when it is named in `--platforms`, for example `bal scan --platforms=sonarqube`. A plain `bal scan` never reports to a platform. The plugin JAR is checked, or downloaded, only when its platform is named, so a `Scan.toml` can declare a platform whose JAR exists only on CI. A path of a named platform that is neither an existing file nor a URL stops the scan with an error.

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

An argument whose name ends with `Path`, such as `sonarProjectPropertiesPath`, that names an existing file relative to the folder of the `Scan.toml` file is passed to the plugin as the absolute path of that file. Any other value, such as a URL or a file the plugin creates, is passed to the plugin unchanged.

The results are always printed and saved before the platforms run, so a failed upload does not hide them. The named platforms then run one after another, and a platform that fails does not stop the others. If any platform fails, the scan lists every failure and exits with code `1`, and the gate is not checked, since an error wins over the gate. A name in `--platforms` that no `[[platform]]` entry declares also fails the scan. In a workspace, platforms can be set only in the `Scan.toml` of each package; a package that does not declare a named platform is skipped with a warning, and the scan fails only when no package declares it.

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

Rules can be either included or excluded, not both: a `[rule]` table that sets both stops the scan with an error that gives its location, and so does using `--include-rules` together with `--exclude-rules`.

Each command-line option adds its rules to its own setting for that run: `--include-rules` adds to `include`, and `--exclude-rules` adds to `exclude`. For example, with `exclude = ["ballerina:1"]` in `Scan.toml`, `--exclude-rules=ballerina:2` excludes both `ballerina:1` and `ballerina:2`. Since rules cannot be included and excluded at the same time, an option that conflicts with the other setting in `Scan.toml` stops the scan as a usage error (exit code `2`): with `exclude = ["ballerina:1"]` in `Scan.toml`, `--include-rules=ballerina:2` is an error.

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

The settings must be written as a `[suppression]` table. Each setting must be a TOML boolean, and the table accepts no other keys; a value such as `"true"` or an unknown key such as `requireReasons` stops the scan with an error that gives its line and column, since it would otherwise leave a requirement off without notice. A suppression that does not meet a requirement fails the scan before any issue is printed, saved or sent to a platform, and the error gives the suppression's location. The command-line options can disable suppressions or switch a requirement on for a single run, but cannot undo the `Scan.toml` settings.

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

Each command-line option replaces only its own setting for that run, with a warning when the value differs from `Scan.toml`. A setting that is not given on the command line is still read from `Scan.toml`: with `failOnSeverity = "HIGH"` in `Scan.toml`, `--fail-on-count=10` fails on 10 issues at or above `HIGH`. `--fail-on-severity=NONE` turns off a gate set in `Scan.toml`. `--fail-on-count` does not turn on a gate that `failOnSeverity = "NONE"` turns off; the scan warns that the count has no effect, and `--fail-on-severity` turns the gate back on. An invalid value or an unknown key in the `[gate]` table stops the scan with an error that gives its line and column.

The gate is checked after the issues are reported, so a scan that fails the gate still saves its reports and sends its issues to the platforms. In a workspace, each package is checked against its own gate with its own issues; see [Configuration for Workspaces](#configuration-for-workspaces).

# Configuration for Test Sources

The test sources of a package, in its `tests` folder and in the `tests` folder of each module, are scanned by default. Users can leave them out by defining the `[analysis]` table in a `Scan.toml` file:

```toml
[analysis]
excludeTests = true
```

- `excludeTests` - Leave the test sources out of the scan. Defaults to `false`. Same as `--exclude-tests=<true|false>`.

When the test sources are excluded, they are not analyzed by the core rules, the issues that static code analyzer plugins report in them are dropped, their `@scan:Suppress` annotations are not applied or reported as unmatched, and the HTML report does not list them. Test sources are still compiled, so a compilation error in a test still fails the scan. The setting must be a TOML boolean, and the table accepts no other keys. The command-line option overrides the `Scan.toml` setting either way for a single run: `--exclude-tests` (or `--exclude-tests=true`) leaves the test sources out, and `--exclude-tests=false` scans them even when `Scan.toml` sets `excludeTests = true`. A single Ballerina file has no test sources, so the option has no effect there.

# Configuration for Workspaces

A workspace can have a `Scan.toml` of its own, which is found the same way as the `Scan.toml` of a package: at the workspace root, or at the `configPath` set in the `[scan]` table of the workspace `Ballerina.toml`. It is used by every package of the workspace that has no `Scan.toml` of its own, and accepts every table a package `Scan.toml` does except `[[platform]]`:

```toml
[rule]
exclude = ["ballerina:1"]

[[analyzer]]
org = "org1"
name = "name1"

[suppression]
requireReason = true

[gate]
failOnSeverity = "HIGH"
failOnCount = 3

[analysis]
excludeTests = true
```

A package uses either its own `Scan.toml` or the workspace one, never both. A package without a `Scan.toml` of its own uses the workspace `Scan.toml`. A package with a `Scan.toml` of its own uses only that file, and none of the workspace settings, not even the tables the package file leaves out; the two files are never merged. For example, with the workspace `Scan.toml` above, a package whose `Scan.toml` sets only `failOnCount = 5` in `[gate]` is gated on 5 issues of any severity, does not exclude `ballerina:1`, does not require suppression reasons and scans its test sources. The scan warns about each workspace setting that the package file leaves out, so that a package does not drop the workspace settings without notice; a setting the package file writes itself is not listed. An empty package `Scan.toml` uses none of the workspace settings.

The command-line options then apply to the file each package uses:

| Table | Command-line options | How the options apply |
|---|---|---|
| `[rule]` | `--include-rules`, `--exclude-rules` | Each one adds its rules to its own setting; a conflict with the other setting in the file is a usage error |
| `[gate]` | `--fail-on-severity`, `--fail-on-count` | Each one replaces its own setting, and the other setting is still read from the file |
| `[suppression]` | `--ignore-suppressions`, `--require-suppression-reason`, `--require-suppression-rule-id` | Each one tightens its own setting, and cannot undo `Scan.toml` |
| `[analysis]` | `--exclude-tests[=<true\|false>]` | Overrides `excludeTests` either way |

The gate applies to each package on its own, with the issues of that package only. Each package that fails its gate is listed in the error, and the scan exits with code `1`. When a rule a package inherits from the workspace `[rule]` table is not provided to the package, for example because only some packages use the analyzer that defines it, the package is named in a warning.

A workspace `Scan.toml` cannot set `[[platform]]`, since each package reports to a project of its own on the platform. The scan stops with an error that gives the location of the table. Each package that reports to a platform sets the platform in its own `Scan.toml`.
