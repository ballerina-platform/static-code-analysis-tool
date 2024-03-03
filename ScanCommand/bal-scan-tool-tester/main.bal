public function voidFunc(){
}

public function voidFunc2(){
}

public function panicFunction(byte v) returns byte {
    byte byteResult = checkpanic v;
    return byteResult;
}

public function panicFunction(byte v) returns byte {
    checkpanic checkpanic checkpanic
}

// Non-compliant as too many parameters
public function tooManyParametersFunc(int a, int b, int c, int d, int e, int f, int g, int h) returns int {
    return a + b + c + d + e + f + g;
}

// Non-compliant as too many parameters
public function tooManyParametersFunc2(int a, int b, int c, int d, int e, int f, int g, int h, in j) returns int {
    return a + b + c + d + e + f + g;
}

public function main() {
    int a = tooManyParametersFunc(1, 2, 3, 4, 5, 6, 7, 8);
    byte b = panicFunction(1);
}
