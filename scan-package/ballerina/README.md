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

The package's compiler plugin checks the form of each rule ID during `bal build` and in the editor: a malformed ID such as `http:1` or an empty rule list is a compile error, and a duplicate rule or a blank reason is a warning. Whether a rule exists is checked when `bal scan` runs, against the core rules of the scan tool in use and the rules of every analyzer it engages; a rule that does not exist fails the scan, and a rule of an analyzer the scan does not engage is reported as a warning, since it cannot be verified.

### Configuration

Both fields are optional by default. A project can require them in `Scan.toml`, in which case `bal scan` fails when a suppression does not meet the requirement, or disable suppressions altogether so that every finding is reported:

```toml
[suppression]
enabled = true
requireReason = true
requireRuleId = true
```
