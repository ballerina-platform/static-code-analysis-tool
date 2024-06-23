# Rule definition

- Rule Title: `Avoid allowing unsafe HTTP methods`
- Rule ID: `undefined`
- Rule Kind: `VULNERABILITY`
- Affecting Modules: `['Ballerina/http']`

### Why is this an issue?

An HTTP resource is safe when used for read-only operations like GET, HEAD, or OPTIONS. An unsafe HTTP resource is used to alter the state of an application, such as modifying the user’s profile on a web application.

Unsafe HTTP resources include POST, PUT, and DELETE.

Enabling both safe and insecure HTTP resources to execute a particular operation on a web application may compromise its security; for instance, CSRF protections are typically designed to safeguard operations executed by insecure HTTP resources.

### Non-compliant Code Example:

```ballerina
listener http:Listener endpoint = new (8080);

service / on endpoint {
    // Sensitive: by default all HTTP methods are allowed
    resource function default deleteRequest(http:Request clientRequest, string username) returns string {
        // state of the application will be changed here
        // ...
    }
}
```

### The solution can be to:

- For every resource in an application, it’s crucial to explicitly define the type of the HTTP resource, ensuring that safe resources are exclusively used for read-only operations.

```ballerina
service / on endpoint {
    // Compliant
    resource function delete deleteRequest(http:Request clientRequest, string username) returns string {
        // state of the application will be changed here
        // ...
    }
}
```

### Additional information

- [OWASP - Top 10 2021 Category A1 - Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- [OWASP - Top 10 2021 Category A4 - Insecure Design](https://owasp.org/Top10/A04_2021-Insecure_Design/)
- [OWASP - Top 10 2017 Category A5 - Broken Access Control](https://owasp.org/www-project-top-ten/2017/A5_2017-Broken_Access_Control)
- [CWE - CWE-352 - Cross-Site Request Forgery (CSRF)](https://cwe.mitre.org/data/definitions/352)
- [OWASP: Cross-Site Request Forgery](https://owasp.org/www-community/attacks/csrf)
- [Spring Security Official Documentation: Use proper HTTP verbs (CSRF protection)](https://docs.spring.io/spring-security/site/docs/5.0.x/reference/html/csrf.html#csrf-use-proper-verbs)