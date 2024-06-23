# Rule definition

- Rule Title: `Passwords should not be stored in plain text or with a fast hashing algorithm`
- Rule ID: `undefined`
- Rule Kind: `CODE_SMELL`
- Affecting Modules: `['Ballerinax/mysql', 'Ballerina/postgresql', 'Ballerina/mongodb', 'Ballerina/java.jdbc']`

### Why is this an issue?

Attackers who would get access to the stored passwords could reuse them without further attacks or with little additional effort. Obtaining clear-text passwords could make the attackers gain unauthorized access to user accounts, potentially leading to various malicious activities.

Sensitive code example:

### Non-compliant Code Example:

```ballerina
listener http:Listener endpoint = new (8080);
configurable string user= ?;
configurable string password = ?;
configurable string database = ?;

final jdbc:Client dbClient;

service / on endpoint {
    function init() returns error? {
        dbClient = check new jdbc:Client(
            url = string `jdbc:h2:./h2/${database}`,
            user = user,
            password = password
        );
    }

    resource function post createUser(http:Request request) returns json|error? {
        json data = check request.getJsonPayload();
        string userName = check data.userName;
        string password = check data.password;

        // Noncompliant
        sql:ExecutionResult result = check dbClient->execute(`INSERT INTO users 
            VALUES (${userName}, ${password})`);

        // ...
    }
}
```

Similar sensitive code is applicable across other modules handling database operations.

### The solution can be to:

- The hashSha512 hashing function in Ballerina is designed to be secure and resistant to various types of attacks, including brute-force and rainbow table attacks. It is adaptive and allows the implementation of a salt.

```ballerina
listener http:Listener endpoint = new (8080);
configurable string user= ?;
configurable string password = ?;
configurable string database = ?;

final jdbc:Client dbClient;

service / on endpoint {
    function init() returns error? {
        dbClient = check new jdbc:Client(
            url = string `jdbc:h2:./h2/${database}`,
            user = user,
            password = password
        );
    }

    resource function post createUser(http:Request request) returns json|error? {
        json data = check request.getJsonPayload();
        string userName = check data.userName;
        string password = check data.password;

        // Compliant
        // Create a salt
        byte[16] salt = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
        foreach int i in 0 ... (salt.length() - 1) {
            salt[i] = <byte>(check random:createIntInRange(0, 255));
        }

        // Hash the password
        byte[] hashedPassword = crypto:hashSha512(password.toBytes(), salt);

        // Add the salt to the hashed password
        byte[] saltedHashPassword = [...salt, ...hashedPassword];

        // convert it to a base 16 string (to save in a DB)
        string saltedHashPasswordString = saltedHashPassword.toBase16();

        // Save to DB
        sql:ExecutionResult result = check dbClient->execute(`INSERT INTO users 
            VALUES (${userName}, ${saltedHashPasswordString})`);

        // ...
    }
}
```

Similar security measures are applied consistently across other modules handling database operations.

### Additional information

- [OWASP - Top 10 2021 Category A2 - Cryptographic Failures](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/)
- [OWASP - Top 10 2021 Category A4 - Insecure Design](https://owasp.org/Top10/A04_2021-Insecure_Design/)
- [OWASP - Top 10 2017 Category A3 - Sensitive Data Exposure](https://owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure)
- [CWE - CWE-256 - Plaintext Storage of a Password](https://cwe.mitre.org/data/definitions/256)
- [CWE - CWE-916 - Use of Password Hash With Insufficient Computational Effort](https://cwe.mitre.org/data/definitions/916)