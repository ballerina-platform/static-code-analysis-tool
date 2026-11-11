# Changelog

All notable changes to the Ballerina scan tool are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each release states the Ballerina distribution it is built against. Releases for older distributions are maintained on the `support/*` branches, which keep their own changelog.

## [Unreleased]

### Fixed

- `ballerina:7` and `ballerina:8` report a comparison with `int:MAX_VALUE`, `int:MIN_VALUE`, `float:Infinity` or `-float:Infinity` on either side of the operator and with any prefix for the `lang.int` and `lang.float` modules. `x <= float:Infinity` is no longer reported as always true, as it is false when `x` is `NaN`, nor is `x <= int:MAX_VALUE` when `x` can be nil. ([#87](https://github.com/ballerina-platform/static-code-analysis-tool/issues/87))
- `ballerina:12` reports invalid ranges written with hexadecimal literals or with values outside the 32-bit range. ([#88](https://github.com/ballerina-platform/static-code-analysis-tool/issues/88))

### Deprecated

- `ScanCodeAnalyzerUtils.isDefinedQualifiedNameReference`, which matches the prefix a module is imported with instead of the module, and is no longer used by the core rules.

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
