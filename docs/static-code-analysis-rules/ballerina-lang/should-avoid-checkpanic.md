# Rule definition

- Rule Title: `Should avoid checkpanic`
- Rule ID: `ballerina:1`
- Rule Kind: `CODE_SMELL`
- Affecting Modules: `['Ballerina/lang']`

### Why is this an issue?

When “checkpanic” is used, the program terminates abruptly with a panic unless it’s handled explicitly along the call stack.

### Non-compliant Code Example:

```ballerina
// Noncompliant
public function checkResult() {
    json result = checkpanic getResult();
}

public function getResult() returns json|error {
    // ...
}
```

### The solution can be to:

- Check and handle the error explicitly

```ballerina
// Compliant
public function checkResult() {
    json|error result = getResult(1, 2);

    if result is error {
        // handle error
    }
}
 
public function getResult() returns json|error {
    // ...
}
```

- Make use of the check keyword, which returns the error or transfers control to an on-fail block, in contrast to checkpanic and panicking if an expression or action evaluates to an error.

```ballerina
// Compliant
public function checkResult() returns error? {
    json result = check getResult(1, 2);
}

public function getResult() returns json|error {
    // ...
}
```

### Additional information

- [Ballerina Checking expression](https://ballerina.io/spec/lang/2021R1/#section_6.33)
- [Usage of checkpanic](https://learn-ballerina.github.io/best_practices/avoid_unnecessary_panic.html#usage-of-checkpanic)