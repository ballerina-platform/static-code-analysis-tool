# Rule definition

- Rule Title: `Avoid using publicly writable directories for file operations without proper access controls`
- Rule ID: `undefined`
- Rule Kind: `VULNERABILITY`
- Affecting Modules: `['Ballerina/file']`

### Why is this an issue?

Operating systems often have global directories with write access granted to any user. These directories serve as temporary storage locations like /tmp in Linux-based systems. However, when an application manipulates files within these directories, it becomes vulnerable to race conditions on filenames. A malicious user may attempt to create a file with a predictable name before the application does. If successful, such an attack could lead to unauthorized access, modification, corruption, or deletion of other files. This risk escalates further if the application operates with elevated permissions.

### Non-compliant Code Example:

```ballerina
string tempFolderPath = os:getEnv("TMP"); // Sensitive
check file:create(tempFolderPath + "/" + "myfile.txt"); // Sensitive
check file:getAbsolutePath(tempFolderPath + "/" + "myfile.txt"); // Sensitive

check file:createTemp("suffix", "prefix"); // Sensitive, will be in the default temporary-file directory.

check file:createTempDir((), "prefix"); // Sensitive, will be in the default temporary-file directory.
```

### The solution can be to:

- Using dedicated sub-folders

```ballerina
check file:create("./myDirectory/myfile.txt"); // Compliant
check file:getAbsolutePath("./myDirectory/myfile.txt"); // Compliant
```

### Additional information

- [OWASP - Top 10 2021 Category A1 - Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- [OWASP - Top 10 2017 Category A5 - Broken Access Control](https://owasp.org/www-project-top-ten/2017/A5_2017-Broken_Access_Control)
- [OWASP - Top 10 2017 Category A3 - Sensitive Data Exposure](https://owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure)
- [CWE - CWE-377 - Insecure Temporary File](https://cwe.mitre.org/data/definitions/377)
- [CWE - CWE-379 - Creation of Temporary File in Directory with Incorrect Permissions](https://cwe.mitre.org/data/definitions/379)
- [OWASP, Insecure Temporary File](https://owasp.org/www-community/vulnerabilities/Insecure_Temporary_File)