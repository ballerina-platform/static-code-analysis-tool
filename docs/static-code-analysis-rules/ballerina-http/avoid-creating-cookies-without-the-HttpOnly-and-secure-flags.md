# Rule definition

- Rule Title: `Avoid creating cookies without the 'HttpOnly' and 'secure' flags`
- Rule ID: `undefined`
- Rule Kind: `VULNERABILITY`
- Affecting Modules: `['Ballerina/http']`

### Why is this an issue?

When a cookie is configured without the HttpOnly attribute set to true or lacks the secure attribute set to true, it presents security vulnerabilities. The HttpOnly attribute is essential as it prevents client-side scripts from accessing the cookie, thereby mitigating Cross-Site Scripting (XSS) attacks aimed at stealing session cookies. Likewise, the secure attribute ensures that the cookie is not transmitted over unencrypted HTTP requests, thereby minimizing the risk of unauthorized interception during man-in-the-middle attacks.

### Non-compliant Code Example:

```ballerina
listener http:Listener endpoint = new (8080);

service / on endpoint {
    resource function get example() returns http:Response|error? {
        http:Response response = new http:Response();

        // Sensitive: this sensitive cookie is created with the httponly flag and secure flag not defined (by default set to false) and so it can be stolen easily in case of XSS vulnerability
        http:Cookie cookie = new ("COOKIENAME", "sensitivedata");
        response.addCookie(cookie);

        return response;
    }
}
```

### The solution can be to:

- When creating session or security-sensitive cookies, it’s recommended to set both the httpOnly and secure flags to true.

```ballerina
listener http:Listener endpoint = new (8080);

service / on endpoint {
    resource function get example() returns http:Response|error? {
        http:Response response = new http:Response();

        // Compliant: this sensitive cookie is protected against theft (HttpOnly=true), the sensitive cookie will not be sent during an unencrypted HTTP request due to the secure flag set to true
        http:Cookie cookie = new (
            "COOKIENAME",
            "sensitivedata",
            httpOnly = true, // Compliant
            secure = true // Compliant
        );
        response.addCookie(cookie);

        return response;
    }
}
```

### Additional information

- [OWASP - Top 10 2021 Category A5 - Security Misconfiguration](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/)
- [OWASP - Top 10 2021 Category A7 - Identification and Authentication Failures](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/)
- [developer.mozilla.org - CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [developer.mozilla.org - Same origin policy](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy)
- [OWASP - Top 10 2017 Category A6 - Security Misconfiguration](https://owasp.org/www-project-top-ten/2017/A6_2017-Security_Misconfiguration\)
- [OWASP HTML5 Security Cheat Sheet - Cross Origin Resource Sharing](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html#cross-origin-resource-sharing)
- [CWE - CWE-346 - Origin Validation Error](https://cwe.mitre.org/data/definitions/346)
- [CWE - CWE-942 - Overly Permissive Cross-domain Whitelist](https://cwe.mitre.org/data/definitions/942)
- [OWASP HttpOnly](https://owasp.org/www-community/HttpOnly)
- [OWASP - Top 10 2017 Category A7 - Cross-Site Scripting (XSS)](https://owasp.org/www-project-top-ten/2017/A7_2017-Cross-Site_Scripting_(XSS))
- [CWE - CWE-1004 - Sensitive Cookie Without 'HttpOnly' Flag](https://cwe.mitre.org/data/definitions/1004)
- [Derived from FindSecBugs rule HTTPONLY_COOKIE](https://find-sec-bugs.github.io/bugs.htm#HTTPONLY_COOKIE)