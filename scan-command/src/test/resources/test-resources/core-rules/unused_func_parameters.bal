import ballerina/http;

function test(int a, int b) returns int {
    return a + b;
}

function test2(int a, int b) returns int => a + b;

function test3(int a, int b, int c) returns int { // warning
    return a + b;
}

function test4(int a, int b, int c) returns int => a + b; // warning

function test5(int a, int b, int c, int d) returns int { // warning * 2
    return test(a, b);
}

function test6(int a, int b, int c, int d) returns int => test(a, b); // warning * 2

function test7(int a, int b, int c = 3) returns int {
    return a + b + c;
}

function test8(int a, int b, int c = 3) returns int { //warning
    return a + b;
}

function test9(int a, int b, int c = 3) returns int => a + b; // warning

function test10(int a, int b, int c = 3) returns int => c + test(a, b);

function test11(int a, int b, int... c) returns string => a.toString(); // warning * 2

function test12(int a, int b, int... c) returns string { // warning * 2
    return a.toString();
}

class A {
    function test(int a, int b) returns int {
        return a + b;
    }

    function test2(int a, int b) returns int => a + b;

    function test3(int a, int b, int c) returns int { // warning
        return a + b;
    }

    function test4(int a, int b, int c) returns int => a + b; // warning

    function test5(int a, int b, int c, int d) returns int { // warning * 2
        return test(a, b);
    }

    function test6(int a, int b, int c, int d) returns int => test(a, b); // warning * 2

    function test7(int a, int b, int c = 3) returns int {
        return a + b + c;
    }

    function test8(int a, int b, int c = 3) returns int { // warning
        return a + b;
    }

    function test9(int a, int b, int c = 3) returns int => a + b; // warning

    function test10(int a, int b, int c = 3) returns int => c + test(a, b);

    function test11(int a, int b, int... c) returns string => a.toString(); // warning * 2

    function test12(int a, int b, int... c) returns string { // warning * 2
        return a.toString();
    }
}

service /a on new http:Listener(8080) {
    resource function get test(int a, int b) returns int {
        return a + b;
    }

    resource function get test2(int a, int b) returns int => a + b;

    resource function get test3(int a, int b, int c) returns int { // warning
        return a + b;
    }

    resource function post test4(int a, int b, int c) returns int => a + b; // warning

    resource function post test5(int a, int b, int c, int d) returns int { // warning * 2
        return test(a, b);
    }

    resource function post test6(int a, int b, int c, int d) returns int => test(a, b); // warning * 2

    resource function post test7(int a, int b, int c = 3) returns int {
        return a + b + c;
    }

    resource function post test8(int a, int b, int c = 3) returns int {  // warning
        return a + b;
    }

    resource function post test9(int a, int b, int c = 3) returns int => a + b; // warning

    resource function post test10(int a, int b, int c = 3) returns int => c + test(a, b);

    resource function post test11(int a, int b, int... c) returns string => a.toString(); // warning * 2

    resource function post test12(int a, int b, int... c) returns string { // warning * 2
        return a.toString();
    }
}

object {} a = object {
    function test(int a, int b) returns int {
        return a + b;
    }

    function test2(int a, int b) returns int => a + b;

    function test3(int a, int b, int c) returns int { // warning
        return a + b;
    }

    function test4(int a, int b, int c) returns int => a + b; // warning

    function test5(int a, int b, int c, int d) returns int { // warning * 2
        return test(a, b);
    }

    function test6(int a, int b, int c, int d) returns int => test(a, b); // warning * 2

    function test7(int a, int b, int c = 3) returns int {
        return a + b + c;
    }

    function test8(int a, int b, int c = 3) returns int { // warning
        return a + b;
    }

    function test9(int a, int b, int c = 3) returns int => a + b; // warning

    function test10(int a, int b, int c = 3) returns int => c + test(a, b);

    function test11(int a, int b, int... c) returns string => a.toString(); // warning * 2

    function test12(int a, int b, int... c) returns string { // warning * 2
        return a.toString();
    }
};

public function main(int a, int b, int c) { // warning
    _ = test(a, c);
    [1,2].forEach(element => ()); // warning
    [1,2].forEach(element => doNothing(element));
}

function doNothing(int a) { // warning
    return;
}

public function t(int a, int b, int c) { // warning
    var fn = function(int a2, int b2) returns int => b;
    int _ = fn(1,2);
}
