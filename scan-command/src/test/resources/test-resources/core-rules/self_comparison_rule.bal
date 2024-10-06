import ballerina/lang.'int as i;

function testSimpleVariableReference() {
    int a = 1;
    int a2 = 1;
    boolean b = true;
    boolean b2 = true;
    string s = "string";

    test2(a <= a);  // warning
    test2(a >= a);  // warning
    test2(a == a);  // warning
    test2(a != a);  // warning
    test2(a < a);  // warning
    test2(a > a);  // warning
    test2(a === a);  // warning
    test2(a !== a);  // warning
    test2(a & a);  // warning
    test2(a | a);  // warning
    test2(b && b);  // warning
    test2(b || b);  // warning

    test2(a <= a2);
    test2(b2 || b);
    test2(s == "string");
}

function testFieldAccessVariableReference() {
    A a = new;
    A b = new;

    test2(a.a <= a.a);  // warning
    test2(a.a >= a.a);  // warning
    test2(a.a == a.a);  // warning
    test2(a.a != a.a);  // warning
    test2(a.a < a.a);  // warning
    test2(a.a > a.a);  // warning
    test2(a.a === a.a);  // warning
    test2(a.a !== a.a);  // warning
    test2(a.a & a.a);  // warning
    test2(a.a | a.a);  // warning
    test2(a.b && a.b);  // warning
    test2(a.b || a.b);  // warning

    test2(a.a <= b.a);
    test2(a.b && b.b);
    test2(a.c == "a.c");
}

function testQualifiedVariableReference() {
    int i = 3;
    int SIGNED8_MIN_VALUE = 10;

    test2(i:SIGNED8_MIN_VALUE <= i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE >= i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE == i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE != i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE < i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE > i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE === i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE !== i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE & i:SIGNED8_MIN_VALUE);  // warning
    test2(i:SIGNED8_MIN_VALUE | i:SIGNED8_MIN_VALUE);  // warning

    test2(SIGNED8_MIN_VALUE <= i:SIGNED8_MIN_VALUE);
    test2(i <= i:SIGNED8_MIN_VALUE);
}

class A {
    int a = 3;
    boolean b = false;
    string c = "string";
}

function test2(any c) {
    _ = c;
}