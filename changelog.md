# Changelog

All notable changes to the Ballerina scan tool are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each release states the Ballerina distribution it is built against. Releases for older distributions are maintained on the `support/*` branches, which keep their own changelog.

## [Unreleased]

### Changed

- `ballerina:13` and `ballerina:14` report a password, passphrase, passcode, `passwd` or `pwd` for any value, as well as a secret whose name mentions one, such as `passwordToken`, or a value in a map of passwords, but other secrets, such as API keys, tokens, credentials, auth values and client secrets, only for a value that looks generated: at least 8 characters, not made of words only, not a run of consecutive characters such as `"12345678"`, and with at least 2.5 bits of entropy per character. Values such as `"true"`, `"Basic "`, `"Authorization"` or `"No Proxy"` given to a key, token or auth target are no longer reported, nor are short or readable values under these names, such as `apiKey = "admin"` or `jwtSecret = "my-secret-key"`, as Sonar's `S6418` and gitleaks also require a random-looking value for these secrets. Such a value is still reported where it reaches a password, and a value built from fixed parts, such as `"sk_live_" + KEY`, including parts held by variables, fields or a configurable's default, is checked as a whole. ([#178](https://github.com/ballerina-platform/static-code-analysis-tool/issues/178))
- The `location` of each issue in `scan_results.json` and in the JSON printed to standard output gives one-based lines and columns, as the SARIF report and the compiler's diagnostics do, and the path of the file relative to the package root, or to the workspace root in the output and combined report of a workspace scan, instead of only its file name. The `scan_results.json` of each workspace package gives paths relative to the root of that package. Consumers that added one to the lines or columns, or matched files by name, need to be updated. `ScanUtils.convertIssuesToJsonString(List, Project, Map)` is added, and the overloads without the project are deprecated, as they can give only the file name. ([#172](https://github.com/ballerina-platform/static-code-analysis-tool/issues/172), [#152](https://github.com/ballerina-platform/static-code-analysis-tool/issues/152))
- The full descriptions of `ballerina:2`, `ballerina:5` and `ballerina:6` state which constructs they skip, and the description of `ballerina:1` explains why a `checkpanic` inside a `trap` is still reported. ([#168](https://github.com/ballerina-platform/static-code-analysis-tool/issues/168))

### Deprecated

- `ScanCodeAnalyzerUtils.isDefinedQualifiedNameReference`, which matches the prefix a module is imported with instead of the module, and is no longer used by the core rules.

### Fixed

- `ballerina:2` no longer reports parameters whose signature is fixed elsewhere: methods that implement a method of an object type included with `*T` or of the type of an object constructor, resource methods of a service declaration typed by a service contract, and anonymous functions used where a function type is expected, such as an argument to `reduce`, to a function of the package or to an optional function-typed parameter, or a member of a list or mapping of functions, including one that is destructured or iterated by a `foreach` that declares a named type. An anonymous function with no expected type, such as the value of a `var` variable or a member of a `var` list, is still reported. ([#169](https://github.com/ballerina-platform/static-code-analysis-tool/issues/169))
- `ballerina:3` to `ballerina:6` no longer report public constructs in test sources, which are not part of the public API of the package. ([#179](https://github.com/ballerina-platform/static-code-analysis-tool/issues/179))
- `ballerina:5` no longer reports `readonly` classes, and `ballerina:6` no longer reports public `service object` types. ([#170](https://github.com/ballerina-platform/static-code-analysis-tool/issues/170), [#171](https://github.com/ballerina-platform/static-code-analysis-tool/issues/171))
- `ballerina:7` and `ballerina:8` report a comparison with `int:MAX_VALUE`, `int:MIN_VALUE`, `float:Infinity` or `-float:Infinity` on either side of the operator, with any prefix for the `lang.int` and `lang.float` modules, and with a `+` sign or braces. `x <= float:Infinity` is no longer reported as always true, as it is false when `x` is `NaN`, nor is `x <= int:MAX_VALUE` when `x` can be nil. ([#87](https://github.com/ballerina-platform/static-code-analysis-tool/issues/87))
- `ballerina:12` reports invalid ranges written with hexadecimal literals or with values outside the 32-bit range. ([#88](https://github.com/ballerina-platform/static-code-analysis-tool/issues/88))
- `ballerina:1` and the other rules check the body of an arrow function, such as `(total, value) => total + checkpanic f()` or `() => checkpanic f()`.
- An issue in the default value of a parameter, such as `int value = checkpanic f()`, is reported once instead of twice.
- `ballerina:2` reports unused parameters of object constructor methods, in a package or a single file. The compiler gives the references of such a parameter twice when the module has an anonymous function, and none otherwise, so the rule now looks up the uses of a parameter in its document. ([#189](https://github.com/ballerina-platform/static-code-analysis-tool/issues/189))
- The SARIF report gives the file name as the `uri` of a single Ballerina file instead of an empty path, and of a file outside the project root instead of a `../` path. ([#174](https://github.com/ballerina-platform/static-code-analysis-tool/pull/174))
- Output printed to `System.out` by a compiler plugin while the package compiles, or by a platform plugin, is written to standard error, so `bal scan > issues.json` saves valid JSON, also with `--platforms`. Before, it was appended to the results on standard output. A process started by a platform plugin writes to the streams directly, so a plugin must not let it inherit standard output, as the platform plugin guide now states. ([#175](https://github.com/ballerina-platform/static-code-analysis-tool/pull/175))

## [0.13.0] - 2026-11-05

Built against Ballerina 2201.13.2.

### Added

- In-source suppression of scan findings with the `@scan:Suppress` annotation. Suppressed findings are left out of the console output and stay reviewable in the saved reports. ([#115](https://github.com/ballerina-platform/static-code-analysis-tool/pull/115))
- Quality gate with the `--fail-on-severity` and `--fail-on-count` options and a `[gate]` table in `Scan.toml`. The scan exits with code `1` when the number of issues at or above the severity reaches the count. In a workspace, each package is gated on its own issues. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))
- `--exclude-tests[=<true|false>]` option and an `[analysis] excludeTests` setting in `Scan.toml` to leave test sources out of the scan. `ScannerContext.isExcludeTests()` lets analyzers skip test sources. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))
- Workspace `Scan.toml`, used by each workspace package that has no `Scan.toml` of its own. The two files are not merged, and `[[platform]]` is not allowed in the workspace file. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))

### Changed

- `ballerina:13` and `ballerina:14` skip values that cannot be secrets, such as booleans, nil defaults, records of configurables, objects and obvious placeholders. ([#147](https://github.com/ballerina-platform/static-code-analysis-tool/pull/147))
- `ballerina:13` and `ballerina:14` match secret names by word instead of by substring, so names such as `author` and `tokenizer` are no longer reported, and names such as `dbCredential` and `privateKey` are. ([#148](https://github.com/ballerina-platform/static-code-analysis-tool/pull/148))
- `ballerina:13` and `ballerina:14` trace the value given to a secret-named target back to its origin before choosing a rule, so values built from configurables or computed at run time are no longer reported as fixed secrets, and a value is reported once instead of at each step of a chain. ([#150](https://github.com/ballerina-platform/static-code-analysis-tool/pull/150))
- `bal scan` exits with `1` when the scan fails or the gate is tripped, `2` on invalid command-line input and `3` when the path is not a Ballerina package, workspace or source file. Errors are printed as `bal scan: error: <message>`, followed by the location of each problem. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))
- `Scan.toml` validation reports every error with its location, and rejects unknown keys and wrongly typed values. An unknown rule ID in `--include-rules` or `--exclude-rules` is now an error, and include and exclude rules cannot be combined. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))
- A platform declared in `Scan.toml` is reported to only when it is named in `--platforms`, and a platform that fails now fails the scan. A relative platform `path` is resolved against the folder of `Scan.toml` instead of the working directory. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))
- Single file, package and workspace scans print their output in the same order, with one blank line between sections: configuration, warnings, progress, issues, issue summary, report locations, platform output and the gate result. Warnings are printed before the section that follows them. The issue summary now comes before the report locations, single file and workspace scans now print a `Running Scans` heading, and the blank lines are written to standard error, so standard output holds only the results. `--platforms` is rejected for a single Ballerina file. ([#163](https://github.com/ballerina-platform/static-code-analysis-tool/pull/163))
- `TestRunner` and the test-runner `ScanCmd` constructor in `scan-command-test-utils` throw `ScanToolException` instead of `BLauncherException`. ([#146](https://github.com/ballerina-platform/static-code-analysis-tool/pull/146))
- The build provisions the Ballerina distribution itself and uses Gradle 9.5.1. ([#116](https://github.com/ballerina-platform/static-code-analysis-tool/pull/116))

### Fixed

- `ballerina:13` and `ballerina:14` check positional arguments to secret-named parameters in `bal scan`, including calls to library functions, remote methods, resource access actions and `new` expressions. Before, these arguments were only checked in the tests. ([#145](https://github.com/ballerina-platform/static-code-analysis-tool/pull/145))
- `ballerina:2` and `ballerina:11` report the location of the parameter or field declaration instead of including its doc comment and annotations. ([#117](https://github.com/ballerina-platform/static-code-analysis-tool/pull/117), [#118](https://github.com/ballerina-platform/static-code-analysis-tool/pull/118))
- The published tool package contains up-to-date tool TOML files. ([#123](https://github.com/ballerina-platform/static-code-analysis-tool/pull/123))

## [0.12.0] - 2026-10-05

Built against Ballerina 2201.13.2.

### Added

- `ballerina:13` (hard-coded secrets) and `ballerina:14` (non-configurable secrets) rules. ([#90](https://github.com/ballerina-platform/static-code-analysis-tool/pull/90))
- Rule metadata: severity, tags, full descriptions and CWE/OWASP mappings for the core rules, included in the JSON and SARIF reports. ([#90](https://github.com/ballerina-platform/static-code-analysis-tool/pull/90))
- Scanning of workspace projects. ([#79](https://github.com/ballerina-platform/static-code-analysis-tool/pull/79))
- Language server entry point for running scans from the IDE, including issues in dependent workspace packages. ([#86](https://github.com/ballerina-platform/static-code-analysis-tool/pull/86))
- HTML report shows rule metadata and supports filtering issues. ([#92](https://github.com/ballerina-platform/static-code-analysis-tool/pull/92))
- HTML report syntax highlighting, a folder tree and per-package views for workspaces. ([#97](https://github.com/ballerina-platform/static-code-analysis-tool/pull/97))

### Changed

- `bal scan` stops without analyzing when the project has compilation errors. ([#91](https://github.com/ballerina-platform/static-code-analysis-tool/pull/91))
- The "Running scans" message is printed earlier in the run. ([#76](https://github.com/ballerina-platform/static-code-analysis-tool/pull/76))
- Rule help links point to the rule ID anchors on `ballerina.io/learn/scan-rules`. ([#103](https://github.com/ballerina-platform/static-code-analysis-tool/pull/103))

### Fixed

- `ballerina:2` no longer reports parameters of external functions. ([#90](https://github.com/ballerina-platform/static-code-analysis-tool/pull/90))

## [0.11.0] - 2025-08-06

Built against Ballerina 2201.12.0.

### Added

- `--format=<json|sarif>` option to choose the report format. ([#70](https://github.com/ballerina-platform/static-code-analysis-tool/pull/70))

## [0.10.0] - 2025-06-02

Built against Ballerina 2201.12.0.

### Added

- Test API (`scan-command-test-utils`) for testing analyzers and rules of compiler plugins.
- Rule documentation in the README.

### Fixed

- Line coverage shown in the HTML report.

## [0.9.0] - 2025-03-17

Built against Ballerina 2201.12.0.

### Added

- `ballerina:3` to `ballerina:6`: non-isolated public functions, methods, classes and objects.
- `ballerina:7` to `ballerina:9`: operations that always evaluate to true, to false, or to the same value.
- `ballerina:10`: variables assigned to themselves.
- `ballerina:11`: unused private class fields.
- `ballerina:12`: invalid range expressions.

### Changed

- Rules are sorted by their numeric ID when listed.

## [0.5.0] - 2024-11-05

Built against Ballerina 2201.10.2.

### Added

- `bal scan` command with console output, a JSON report and an HTML report (`--scan-report`).
- `ballerina:1` (avoid `checkpanic`) and `ballerina:2` (unused function parameters) rules.
- Rule selection with `--include-rules`, `--exclude-rules` and `--list-rules`, and configuration through `Scan.toml`, including remote configurations.
- Loading analyzers from compiler plugins and reporting to static code analysis platforms with `--platforms`.

[Unreleased]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.13.0...HEAD
[0.13.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.5.0...v0.9.0
[0.5.0]: https://github.com/ballerina-platform/static-code-analysis-tool/releases/tag/v0.5.0
