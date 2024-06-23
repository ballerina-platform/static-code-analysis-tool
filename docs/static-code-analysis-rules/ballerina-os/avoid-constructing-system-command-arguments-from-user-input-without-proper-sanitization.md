# Rule definition

- Rule Title: `Avoid constructing system command arguments from user input without proper sanitization`
- Rule ID: `undefined`
- Rule Kind: `VULNERABILITY`
- Affecting Modules: `['Ballerina/os']`

### Why is this an issue?

Arguments of system commands are processed by the executed program. The arguments are usually used to configure and influence the behavior of the programs. Control over a single argument might be enough for an attacker to trigger dangerous features like executing arbitrary commands or writing files into specific directories.

Arguments like -delete or -exec for the find command can alter the expected behavior and result in vulnerabilities:

### Non-compliant Code Example:

```ballerina
string terminalPath = ...;
string input = request.getQueryParamValue("input").toString();
string[] cmd = [..., input];

// Sensitive
os:Process result = check os:exec({
    value: terminalPath, 
    arguments: cmd
});
```

### The solution can be to:

- Use an allow-list to restrict the arguments to trusted values:

```ballerina
string terminalPath = ...;
string input = request.getQueryParamValue("input").toString();
string[] cmd = [..., input];
string[] allowed = ["main", "main.bal", "bal"];

// Compliant
if allowed.some(keyword => keyword.equalsIgnoreCaseAscii(input)) {
    os:Process result = check os:exec({
        value: terminalPath, 
        arguments: cmd
    });
}
```

### Additional information

- [OWASP - Top 10 2021 Category A3 - Injection](https://owasp.org/Top10/A03_2021-Injection/)
- [OWASP - Top 10 2017 Category A1 - Injection](https://owasp.org/www-project-top-ten/2017/A1_2017-Injection)
- [CWE - CWE-88 - Argument Injection or Modification](https://cwe.mitre.org/data/definitions/88)
- [CVE-2021-29472 - PHP Supply Chain Attack on Composer](https://blog.sonarsource.com/php-supply-chain-attack-on-composer?_gl=1*8q1b2c*_gcl_au*MTQ0MzcyNzE1Ni4xNzEzNDAwMTc0*_ga*ODk5ODY4NDU1LjE2ODk5Mjg1MjA.*_ga_9JZ0GZ5TC6*MTcxNzkyODY0Mi4yNDQuMS4xNzE3OTM5MjUwLjYwLjAuMA)