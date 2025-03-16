function testSimpleVariableReference() {
    int a = 1;
    int a2 = 1;
    string|int a3 = 1;

    a = a;  // warning
    a += a;  // warning
    a -= a;  // warning
    a *= a;  // warning
    a /= a;  // warning
    a &= a;  // warning
    a |= a;  // warning
    a ^= a;  // warning
    a <<= a;  // warning
    a >>= a;  // warning
    a >>>= a;  // warning

    a = a2;
    a3 = "a3";

    a += a2;
    a += 1;
}

function testFieldAccessVariableReference() {
    A a = new;
    A b = new;

    a.a = a.a; // warning
    a.a += a.a;  // warning
    a.a -= a.a;  // warning
    a.a *= a.a;  // warning
    a.a /= a.a;  // warning
    a.a &= a.a;  // warning
    a.a |= a.a;  // warning
    a.a ^= a.a;  // warning
    a.a <<= a.a;  // warning
    a.a >>= a.a;  // warning
    a.a >>>= a.a;  // warning

    a.a = b.a;
    a.a3 = "a.a3";
    a.a += 1;
    a.a += a.a4;
}

class A {
    int a = 3;
    boolean b = false;
    string c = "string";
    int|string a3 = 3;
    int a4 = 3;
}

function test2(any c) {
    _ = c;
}

function t() {
    map<json> a = {};
    map<json> b = {};

    a["a"] = a["a"]; // warning
    a["a"] += a["a"];  // warning
    a["a"] -= a["a"];  // warning
    a["a"] *= a["a"];  // warning
    a["a"] /= a["a"];  // warning
    a["a"] &= a["a"];  // warning
    a["a"] |= a["a"];  // warning
    a["a"] ^= a["a"];  // warning
    a["a"] <<= a["a"];  // warning
    a["a"] >>= a["a"];  // warning
    a["a"] >>>= a["a"];  // warning

    a["a"] = b["a"];
    a.["a3"] = "a.[\"a3\"]";
    a.["a"] += 1;
    a.["a"] += a.["a4"];
}