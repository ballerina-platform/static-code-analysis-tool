import ballerina/lang.'int as i;

function testSimpleVariableReference() {
    int a = 1;
    int a2 = 1;
    boolean b = true;
    boolean b2 = true;
    string s = "string";
    A a = new;
    A b = new;
    int i = 3;
    int SIGNED8_MIN_VALUE = 10;
    map<int> a = {};
    map<int> b = {};

    test2(a <= a);  // warning
    test2(a >= a);  // warning
    test2(a == a);  // warning
    test2(a === a);  // warning
    test2(a.a <= a.a);  // warning
    test2(a.a >= a.a);  // warning
    test2(a.a == a.a);  // warning
    test2(a.a === a.a);  // warning
    test2(i:SIGNED8_MIN_VALUE <= i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE >= i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE == i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE === i:SIGNED8_MIN_VALUE);  // warning
    test2(a["a"] <= a["a"]);  // warning
    test2(a["a"] >= a["a"]);  // warning
    test2(a["a"] == a["a"]);  // warning
    test2(a["a"] === a["a"]);  // warning
    result = x >= int:MIN_VALUE; // warning
    result = x <= int:MAX_VALUE; // warning
    result = y <= float:Infinity; // warning
    result = b || true; // warning
    result2 = x | -1;// warning

    test2(a <= a2);
    test2(b2 || b);
    test2(s == "string");
    test2(a.a <= b.a);
    test2(a.b && b.b);
    test2(a.c == "a.c");

    test2(SIGNED8_MIN_VALUE <= i:SIGNED8_MIN_VALUE);
    test2(i <= i:SIGNED8_MIN_VALUE);

    test2(a["a"] <= b["a"]);
    test2(a["a"] || b["a"]);
    test2(a["a"] == "b[\"a\"]");

    test2(a["a"] <= a["b"]);
    test2(a["a"] || a["b"]);
    test2(a["a"] == "a[\"b\"]");
}

class A {
    int a = 3;
    boolean b = false;
    string c = "string";
}

function test2(any c) {
    _ = c;
}

function testOperators() {
    int x = 1;
    float y = 2.3f;
    boolean result;
}

function testRelationaOperatorsWithoutWarnings() {
    string x = "1";
    string y = "2.3f";
    boolean result;

    // Arithmetic operators
    result = x < "int:MIN_VALUE";
    result = x >= "int:MIN_VALUE";

    result = x <= "int:MAX_VALUE";
    result = x > "int:MAX_VALUE";

    result = y <= "float:Infinity";
    result = y > "float:Infinity";

    // Logical operators
    boolean 'false = false;
    boolean 'true = true;
    boolean b = true;

    result = b && 'false;
    result = b || 'true;
    result = b && 'true;
    result = b || 'false;

    // Bitwise operators
    int '0 = 0;
    int \-1 = -1;
    int x2 = 1;

    int result2 = x2 | '0;
    result2 = x2 & \-1;
    result2 = x2 | \-1;
    result2 = x2 & '0;
    result2 = x2 | 1;
    result2 = x2 & 1;


    int x = 1;
    float y = 2.3f;
    boolean result;

    // Arithmetic operators
    result = x > int:MIN_VALUE;
    result = x <= int:MIN_VALUE;

    result = x >= int:MAX_VALUE;
    result = x < int:MAX_VALUE;

    result = y >= float:Infinity;
    result = y < float:Infinity;

    // Bitwise operators
    int result2 = x | 1;
    result2 = x & --1;
}
