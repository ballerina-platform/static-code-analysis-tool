# Ballerina Static Code Analysis Rules

This document points to the static code analysis rule files created under each Ballerina module directory. The `common` directory contains rules that affect multiple Ballerina modules.

Each rule file follows the following format:

```md
# Rule definition

- Rule Title: `Title of the rule`
- Rule ID: `fully qualified identifier of the rule/undefined`
- Rule Kind: `CODE_SMELL/BUG/VULNERABILITY`
- Affecting Modules: `['Ballerina/moduleName', 'Ballerinax/moduleName', 'Org/moduleName']`

### Why is this an issue?

Description of the issue.

### Non-compliant Code Example:

[Non-compliant code example]

### The solution can be to:

- Solution 1

[Solution 1 code example]

- Solution 2

[Solution 2 code example]

### Additional information

- Link 1 to additional information
- Link 2 to additional information
```

# Rules

- ballerina/lang:
  - [Should avoid checkpanic](ballerina-lang/should-avoid-checkpanic.md)
  - [Functions should not have too many parameters](ballerina-lang/functions-should-not-have-too-many-parameters.md)
  - [Functions should not be empty](ballerina-lang/functions-should-not-be-empty.md)

- ballerina/file:
  - [Avoid using publicly writable directories for file operations without proper access controls](ballerina-file/avoid-using-publicly-writable-directories-for-file-operations-without-proper-access-controls.md)
  - [I/O function calls should not be vulnerable to path injection attacks](ballerina-file/io-function-calls-should-not-be-vulnerable-to-path-injection-attacks.md)

- ballerina/http:
  - [Avoid allowing unsafe HTTP methods](ballerina-http/avoid-allowing-unsafe-HTTP-methods.md)
  - [Avoid permissive Cross-Origin Resource Sharing](ballerina-http/avoid-permissive-cross-origin-resource-sharing.md)
  - [Avoid creating cookies without the HttpOnly and secure flags](ballerina-http/avoid-creating-cookies-without-the-HttpOnly-and-secure-flags.md)

- ballerina/os:
  - [Avoid constructing system command arguments from user input without proper sanitization](ballerina-os/avoid-constructing-system-command-arguments-from-user-input-without-proper-sanitization.md) 

- ballerinax/mysql:
  - [A secure password should be used when connecting to a database](ballerinax-mysql/a-secure-password-should-be-used-when-connecting-to-a-database.md) 

- common:
  - [Passwords should not be stored in plain text or with a fast hashing algorithm](common/passwords-should-not-be-stored-in-plain-text-or-with-a-fast-hashing-algorithm.md) 

# References

- [Ballerina Static Code Analysis Rules](https://docs.google.com/document/d/16ynMq1J8Ua4OnofghVEzJH7N7JYz6axeKK4X8OFm3Lo/edit?usp=sharing)