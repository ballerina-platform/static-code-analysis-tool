# Changelog

All notable changes to the Ballerina scan tool are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each release states the Ballerina distribution it is built against. This branch maintains the 0.11.x line for Ballerina 2201.12 (Update 12). Releases for newer distributions are made from `main`.

## [Unreleased]

Built against Ballerina 2201.12.0.

### Added

- In-source suppression of scan findings with the `@scan:Suppress` annotation. Suppressed findings are left out of the console output and stay reviewable in the saved reports. ([#119](https://github.com/ballerina-platform/static-code-analysis-tool/pull/119), backport of [#115](https://github.com/ballerina-platform/static-code-analysis-tool/pull/115))

### Changed

- The "Running scans" message is printed earlier in the run. ([#76](https://github.com/ballerina-platform/static-code-analysis-tool/pull/76))
- The build provisions the Ballerina distribution itself and uses Gradle 9.5.1. ([#120](https://github.com/ballerina-platform/static-code-analysis-tool/pull/120))

### Fixed

- `ballerina:2` and `ballerina:11` report the location of the parameter or field declaration instead of including its doc comment and annotations. ([#120](https://github.com/ballerina-platform/static-code-analysis-tool/pull/120), backport of [#117](https://github.com/ballerina-platform/static-code-analysis-tool/pull/117) and [#118](https://github.com/ballerina-platform/static-code-analysis-tool/pull/118))
- `ballerina:2` no longer reports parameters of external functions. ([#120](https://github.com/ballerina-platform/static-code-analysis-tool/pull/120))
- The published tool package contains up-to-date tool TOML files. ([#122](https://github.com/ballerina-platform/static-code-analysis-tool/pull/122))

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

[Unreleased]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.11.0...support/dist-2201.12/0.11.x
[0.11.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.5.0...v0.9.0
[0.5.0]: https://github.com/ballerina-platform/static-code-analysis-tool/releases/tag/v0.5.0
