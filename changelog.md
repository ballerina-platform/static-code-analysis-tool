# Changelog

All notable changes to the Ballerina scan tool are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each release states the Ballerina distribution it is built against. Releases for older distributions are maintained on the `support/*` branches, which keep their own changelog.

## [Unreleased]

### Added

- In-source suppression of scan findings with the `@scan:Suppress` annotation. Suppressed findings are left out of the console output and stay reviewable in the saved reports. ([#115](https://github.com/ballerina-platform/static-code-analysis-tool/pull/115))

### Changed

- `ballerina:13` and `ballerina:14` skip values that cannot be secrets, such as booleans, nil defaults, records of configurables, objects and obvious placeholders. ([#147](https://github.com/ballerina-platform/static-code-analysis-tool/pull/147))
- `ballerina:13` and `ballerina:14` match secret names by word instead of by substring, so names such as `author` and `tokenizer` are no longer reported, and names such as `dbCredential` and `privateKey` are. ([#148](https://github.com/ballerina-platform/static-code-analysis-tool/pull/148))
- `ballerina:13` and `ballerina:14` trace the value given to a secret-named target back to its origin before choosing a rule, so values built from configurables or computed at run time are no longer reported as fixed secrets, and a value is reported once instead of at each step of a chain. ([#150](https://github.com/ballerina-platform/static-code-analysis-tool/pull/150))
- The build provisions the Ballerina distribution itself and uses Gradle 9.5.1. ([#116](https://github.com/ballerina-platform/static-code-analysis-tool/pull/116))

### Fixed

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

[Unreleased]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.12.0...HEAD
[0.12.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.5.0...v0.9.0
[0.5.0]: https://github.com/ballerina-platform/static-code-analysis-tool/releases/tag/v0.5.0
