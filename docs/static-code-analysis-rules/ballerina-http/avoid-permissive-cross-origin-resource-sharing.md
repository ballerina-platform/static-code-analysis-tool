# Rule definition

- Rule Title: `Avoid permissive Cross-Origin Resource Sharing`
- Rule ID: `undefined`
- Rule Kind: `VULNERABILITY`
- Affecting Modules: `['Ballerina/http']`

### Why is this an issue?

Having a permissive Cross-Origin Resource Sharing policy is security-sensitive, and it has led in the past to the following vulnerabilities.

- [CVE-2018-0269](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-0269)
- [CVE-2017-14460](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-14460)

Browsers enforce the same-origin policy by default, as a security measure, preventing JavaScript frontends from making cross-origin HTTP requests to resources with different origins (domains, protocols, or ports). However, the target resource can include additional HTTP headers in its response, known as CORS headers, which serve as directives for the browser and modify the access control policy, effectively relaxing the same-origin policy.

### Non-compliant Code Example:

```ballerina
listener http:Listener endpoint = new (8080);

service / on endpoint {
    // Noncompliant
    @http:ResourceConfig {
        cors: {
            allowOrigins: ["*"] // Sensitive
        }
    }

    resource function get example() returns http:Response|error? {
        // Return response
    }
}
```

### The solution can be to:

- The resource configuration should be configured exclusively for trusted origins  and specific resources

```ballerina
listener http:Listener endpoint = new (8080);

service / on endpoint {
    @http:ResourceConfig {
        cors: {
            allowOrigins: ["trustedwebsite.com"] // Compliant
        }
    }

    resource function get example() returns http:Response|error? {
        // Return response
    }
}
```

### Additional information

- [OWASP - Top 10 2021 Category A5 - Security Misconfiguration](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/)
- [OWASP - Top 10 2021 Category A7 - Identification and Authentication Failures](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/)
- [developer.mozilla.org - CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [developer.mozilla.org - Same origin policy](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy)
- [OWASP - Top 10 2017 Category A6 - Security Misconfiguration](https://owasp.org/www-project-top-ten/2017/A6_2017-Security_Misconfiguration)
- [OWASP HTML5 Security Cheat Sheet - Cross Origin Resource Sharing](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html#cross-origin-resource-sharing)
- [CWE - CWE-346 - Origin Validation Error](https://cwe.mitre.org/data/definitions/346)
- [CWE - CWE-942 - Overly Permissive Cross-domain Whitelist](https://cwe.mitre.org/data/definitions/942)