# Changelog

All notable changes to the Ballerina scan tool are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each release states the Ballerina distribution it is built against. This branch maintains the 0.11.x line for Ballerina 2201.12 (Update 12). Releases for newer distributions are made from `main`.

## [Unreleased]

Built against Ballerina 2201.12.0.

### Changed

- The `location` of each issue in `scan_results.json` and in the JSON printed to standard output gives one-based lines and columns, as the SARIF report and the compiler's diagnostics do, and the path of the file relative to the package root instead of only its file name. Consumers that added one to the lines or columns, or matched files by name, need to be updated. `ScanUtils.convertIssuesToJsonString(List, Project, Map)` is added, and `convertIssuesToJsonString(List)` is deprecated, as it can give only the file name. (backport of [#174](https://github.com/ballerina-platform/static-code-analysis-tool/pull/174); [#172](https://github.com/ballerina-platform/static-code-analysis-tool/issues/172), [#152](https://github.com/ballerina-platform/static-code-analysis-tool/issues/152))

### Deprecated

- `ScanCodeAnalyzerUtils.isDefinedQualifiedNameReference`, which matches the prefix a module is imported with instead of the module, and is no longer used by the core rules.

### Fixed

- The JSON and SARIF reports, the JSON and SARIF printed to standard output and the HTML report list issues in the same order in every run: by file path, start line, start column, end line, end column and rule ID. Before, the order followed the order the modules were scanned in, which changed between runs. The HTML report also lists its files by path. (backport of [#207](https://github.com/ballerina-platform/static-code-analysis-tool/pull/207); [#193](https://github.com/ballerina-platform/static-code-analysis-tool/issues/193))
- Output printed to `System.out` by a compiler plugin while the package compiles, or by a platform plugin, is written to standard error, so `bal scan > issues.json` saves valid JSON, also with `--platforms`. Before, it was appended to the results on standard output. A process started by a platform plugin writes to the streams directly, so a plugin must not let it inherit standard output, as the platform plugin guide now states. (backport of [#175](https://github.com/ballerina-platform/static-code-analysis-tool/pull/175))
- `ballerina:2` no longer reports parameters whose signature is fixed elsewhere: methods that implement a method of an object type included with `*T` or of the type of an object constructor, resource methods of a service declaration typed by a service contract, and anonymous functions used where a function type is expected, such as an argument to `reduce`, to a function of the package or to an optional function-typed parameter, or a member of a list or mapping of functions, including one that is destructured or iterated by a `foreach` that declares a named type. An anonymous function with no expected type, such as the value of a `var` variable or a member of a `var` list, is still reported. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173); [#169](https://github.com/ballerina-platform/static-code-analysis-tool/issues/169))
- `ballerina:3` to `ballerina:6` no longer report public constructs in test sources, which are not part of the public API of the package. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173); [#179](https://github.com/ballerina-platform/static-code-analysis-tool/issues/179))
- `ballerina:5` no longer reports `readonly` classes, and `ballerina:6` no longer reports public `service object` types. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173); [#170](https://github.com/ballerina-platform/static-code-analysis-tool/issues/170), [#171](https://github.com/ballerina-platform/static-code-analysis-tool/issues/171))
- `ballerina:7` and `ballerina:8` report a comparison with `int:MAX_VALUE`, `int:MIN_VALUE`, `float:Infinity` or `-float:Infinity` on either side of the operator, with any prefix for the `lang.int` and `lang.float` modules, and with a `+` sign or braces. `x <= float:Infinity` is no longer reported as always true, as it is false when `x` is `NaN`, nor is `x <= int:MAX_VALUE` when `x` can be nil. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173); [#87](https://github.com/ballerina-platform/static-code-analysis-tool/issues/87))
- `ballerina:12` reports invalid ranges written with hexadecimal literals or with values outside the 32-bit range. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173); [#88](https://github.com/ballerina-platform/static-code-analysis-tool/issues/88))
- `ballerina:1` and the other rules check the body of an arrow function, such as `(total, value) => total + checkpanic f()` or `() => checkpanic f()`. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173))
- An issue in the default value of a parameter, such as `int value = checkpanic f()`, is reported once instead of twice. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173))
- `ballerina:2` reports unused parameters of object constructor methods, whose references the compiler gives twice. (backport of [#173](https://github.com/ballerina-platform/static-code-analysis-tool/pull/173))
- The SARIF report gives the file name as the `uri` of a single Ballerina file instead of an empty path. (backport of [#174](https://github.com/ballerina-platform/static-code-analysis-tool/pull/174))

## [0.11.1] - 2026-11-10

Built against Ballerina 2201.12.0.

### Added

- In-source suppression of scan findings with the `@scan:Suppress` annotation. Suppressed findings are left out of the console output and stay reviewable in the saved reports. ([#119](https://github.com/ballerina-platform/static-code-analysis-tool/pull/119), backport of [#115](https://github.com/ballerina-platform/static-code-analysis-tool/pull/115))
- Each issue in the JSON and SARIF output carries the source text it covers, as `location.snippet` and `region.snippet.text`.

### Changed

- The "Running scans" message is printed earlier in the run. ([#76](https://github.com/ballerina-platform/static-code-analysis-tool/pull/76))
- Single file and package scans print their output in the same order, with one blank line between sections: configuration, warnings, progress, issues, issue summary, report locations, platform output and the gate result. Warnings are printed before the section that follows them, and the blank lines are written to standard error, so standard output holds only the results. `--platforms` is rejected for a single Ballerina file.
- `--include-rules` and `--exclude-rules` add to `include` and `exclude` in `Scan.toml` instead of replacing them.
- The HTML scan report uses the redesigned report of 0.12.0: syntax highlighting, a folder tree that lists every scanned file, issue filtering, and the severity of each issue, which is the one the gate counts its rule kind as. (backport of [#92](https://github.com/ballerina-platform/static-code-analysis-tool/pull/92), [#97](https://github.com/ballerina-platform/static-code-analysis-tool/pull/97) and [#102](https://github.com/ballerina-platform/static-code-analysis-tool/pull/102))
- The build provisions the Ballerina distribution itself and uses Gradle 9.5.1. ([#120](https://github.com/ballerina-platform/static-code-analysis-tool/pull/120))
- A package is scanned, or has its rules listed, only when its dependencies are locked in `Dependencies.toml`; otherwise the scan asks to run `bal build` first.
- `bal scan --list-rules` prints "Resolving scan rules from package dependencies" instead of "Running Scans", and fails when the package does not compile.
- The `fileName` of an issue in a single Ballerina file is the file name, without a `./` prefix.

### Fixed

- `ballerina:2` and `ballerina:11` report the location of the parameter or field declaration instead of including its doc comment and annotations. ([#120](https://github.com/ballerina-platform/static-code-analysis-tool/pull/120), backport of [#117](https://github.com/ballerina-platform/static-code-analysis-tool/pull/117) and [#118](https://github.com/ballerina-platform/static-code-analysis-tool/pull/118))
- `ballerina:2` no longer reports parameters of external functions. ([#120](https://github.com/ballerina-platform/static-code-analysis-tool/pull/120))
- The published tool package contains up-to-date tool TOML files. ([#122](https://github.com/ballerina-platform/static-code-analysis-tool/pull/122))
- `bal scan --exclude-tests` no longer fails with `NoClassDefFoundError` on the picocli version bundled with the `bal` launcher.
- `bal scan .` scans the package in the current directory instead of failing with "'.' is already within a Ballerina package".

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

[Unreleased]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.11.1...support/dist-2201.12/0.11.x
[0.11.1]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.11.0...v0.11.1
[0.11.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/ballerina-platform/static-code-analysis-tool/compare/v0.5.0...v0.9.0
[0.5.0]: https://github.com/ballerina-platform/static-code-analysis-tool/releases/tag/v0.5.0
