## Overview

This package provides the `@scan:Suppress` annotation, which accepts individual findings of the `bal scan` static code analysis tool at the construct that contains them.

```ballerina
import ballerina/scan;

@scan:Suppress {rules: ["ballerina/crypto:2"], reason: "Legacy hash kept for the migration window, SEC-1042"}
string hashed = check crypto:hashBcrypt(password, 4);
```

A suppressed finding is not removed from the scan results. It is reported with its justification, so that every accepted risk remains visible and reviewable.

### Fields

- `rules` - The rules being accepted, as printed by `bal scan`, such as `ballerina:1` or `ballerina/http:2`. When absent, every finding in the construct is accepted.
- `reason` - Why the findings are accepted. Optional unless the project enables `requireReason`.

### Placement

The annotation applies to findings reported within the construct it is attached to: a variable or constant declaration, a function, method or resource, a parameter, a field, a type, a class or a service. A suppression on a service, class or object type does not reach findings inside its members.

A finding inside a statement that is not a declaration can be suppressed by turning the statement into one, for example `var _ = check f();` instead of `check f();`.

### Rule ID validation

The package's compiler plugin checks each rule ID against the rules `bal scan` can report for the importing package: the core rules of the scan tool, and the rules of each direct dependency's analyzer. An unknown rule is a compile error. A rule of an analyzer declared only in `Scan.toml` is reported as a warning during `bal build`, and is checked strictly when `bal scan` runs.

### Requiring a reason or a rule ID

Both fields are optional by default. A project can require them in `Scan.toml`; `bal scan` then fails when a suppression does not meet the requirement:

```toml
[suppression]
requireReason = true
requireRuleId = true
```
