# Rule definition

- Rule Title: `I/O function calls should not be vulnerable to path injection attacks`
- Rule ID: `undefined`
- Rule Kind: `VULNERABILITY`
- Affecting Modules: `['Ballerina/file']`

### Why is this an issue?

Path injections occur when an application constructs a file path using untrusted data without first validating the path.

A malicious user can inject specially crafted values, like "../", to alter the intended path. This manipulation may lead the path to resolve to a location within the filesystem where the user typically wouldn't have access.

### Non-compliant Code Example:

```ballerina
listener http:Listener endpoint = new (8080);
string targetDirectory = "./path/to/target/directory/";

service / on endpoint {
    resource function get deleteFile(string fileName) returns string|error {
        // Noncompliant
        check file:remove(targetDirectory + fileName);

        // ...
    }
}
```

### The solution can be to:

- Conduct validation of canonical paths.

```ballerina
listener http:Listener endpoint = new (8080);
string targetDirectory = "./path/to/target/directory/";

service / on endpoint {
    resource function get deleteFile(string fileName) returns string|error {
        // Compliant
        // Retrieve the normalized absolute path of the user provided file
        string absoluteUserFilePath = check file:getAbsolutePath(targetDirectory + fileName);
        string normalizedAbsoluteUserFilePath = check file:normalizePath(absoluteUserFilePath, file:CLEAN);

        // Check whether the user provided file exists
        boolean fileExists = check file:test(normalizedAbsoluteUserFilePath, file:EXISTS);
        if !fileExists {
            return "File does not exist!";
        }

        // Retrieve the normalized absolute path of parent directory of the user provided file
        string canonicalDestinationPath = check file:parentPath(normalizedAbsoluteUserFilePath);
        string normalizedCanonicalDestinationPath = check file:normalizePath(canonicalDestinationPath, file:CLEAN);

        // Retrieve the normalized absolute path of the target directory
        string absoluteTargetFilePath = check file:getAbsolutePath(targetDirectory);
        string normalizedTargetDirectoryPath = check file:normalizePath(absoluteTargetFilePath, file:CLEAN);

        // Perform comparison of user provided file path and target directory path
        boolean dirMatch = normalizedTargetDirectoryPath.equalsIgnoreCaseAscii(normalizedCanonicalDestinationPath);
        if !dirMatch {
            return "Entry is not in the target directory!";
        }

        check file:remove(normalizedAbsoluteUserFilePath);

        // ...
    }
}
```

### Additional information

- [OWASP - Top 10 2021 Category A1 - Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- [OWASP - Top 10 2021 Category A3 - Injection](https://owasp.org/Top10/A03_2021-Injection/)
- [OWASP - Top 10 2017 Category A1 - Injection](https://owasp.org/www-project-top-ten/2017/A1_2017-Injection)
- [OWASP - Top 10 2017 Category A5 - Broken Access Control](https://owasp.org/www-project-top-ten/2017/A5_2017-Broken_Access_Control)
- [CWE - CWE-20 - Improper Input Validation](https://cwe.mitre.org/data/definitions/20)
- [CWE - CWE-22 - Improper Limitation of a Pathname to a Restricted Directory ('Path Traversal')](https://cwe.mitre.org/data/definitions/22)