# Rule definition

- Rule Title: `A secure password should be used when connecting to a database`
- Rule ID: `undefined`
- Rule Kind: `CODE_SMELL`
- Affecting Modules: `['Ballerinax/mysql']`

### Why is this an issue?

When a database lacks authentication requirements, it opens the door for unrestricted access and manipulation of its stored data. Exploiting this vulnerability usually involves identifying the target database and establishing a connection to it without the necessity of any authentication credentials.

### Non-compliant Code Example:

```ballerina
configurable string user = ?;
configurable string host = ?;
configurable int port = ?;
configurable string database = ?;

public function connectToDatabase() {
    mysql:Client|error dbClient = new (host = host,
        user = user,
        password = "", // Noncompliant
        port = port,
        database = database
    );
    
    // ...
}
```

### The solution can be to:

- Using a robust password sourced from configurations. The 'password’' property is configured in a configuration file during deployment, and its value should be both strong and unique for each database.

```ballerina
configurable string user = ?;
configurable string password = ?; // Compliant
configurable string host = ?;
configurable int port = ?;
configurable string database = ?;

public function connectToDatabase() {
    mysql:Client|error dbClient = new (host = host,
        user = user,
        password = password, // Compliant
        port = port,
        database = database
    );

    // ...
}
```

### Additional information

- [OWASP - Top 10 2021 Category A2 - Cryptographic Failures](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/)
- [OWASP - Top 10 2021 Category A4 - Insecure Design](https://owasp.org/Top10/A04_2021-Insecure_Design/)
- [OWASP - Top 10 2017 Category A3 - Sensitive Data Exposure](https://owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure)
- [CWE - CWE-256 - Plaintext Storage of a Password](https://cwe.mitre.org/data/definitions/256)
- [CWE - CWE-916 - Use of Password Hash With Insufficient Computational Effort](https://cwe.mitre.org/data/definitions/916)