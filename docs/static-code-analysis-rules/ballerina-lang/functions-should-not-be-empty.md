# Rule definition

- Rule Title: `Functions should not be empty`
- Rule ID: `undefined`
- Rule Kind: `CODE_SMELL`
- Affecting Modules: `['Ballerina/lang']`

### Why is this an issue?

There are multiple factors contributing to a function lacking a function body:

- It’s an unintentional omission and should be fixed to prevent unexpected behavior in production
- It either hasn't been implemented yet or won't be supported at all. In such instances, a panic should be raised.

### Non-compliant Code Example:

```ballerina
// Noncompliant
public function emptyFunction(){

}
```

### The solution can be to:

### Additional information