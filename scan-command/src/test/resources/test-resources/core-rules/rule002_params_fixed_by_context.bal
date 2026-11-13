// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;

public type Fetcher isolated client object {
    remote isolated function fetch(string path) returns string|error;
    isolated function close(boolean force);
};

public type CachingFetcher isolated client object {
    *Fetcher;
};

public isolated client class UnsupportedFetcher {
    *Fetcher;

    remote isolated function fetch(string path) returns string|error {
        return error("not supported");
    }

    isolated function close(boolean force) {
    }

    isolated function describe(string format) returns string { // warning
        return "unsupported";
    }
}

public isolated client class UnsupportedCachingFetcher {
    *CachingFetcher;

    remote isolated function fetch(string path) returns string|error {
        return error("not supported");
    }

    isolated function close(boolean force) {
    }
}

type Greeter service object {
    *http:ServiceContract;
    resource function get greeting(string name) returns string;
};

service Greeter on new http:Listener(9091) {
    resource function get greeting(string name) returns string {
        return "Hello";
    }
}

service /untyped on new http:Listener(9092) {
    resource function get greeting(string name) returns string { // warning
        return "Hello";
    }
}

function count(int[] values) returns int {
    return values.reduce(function(int total, int value) returns int => total + 1, 0);
}

function countWithArrowFunction(int[] values) returns int {
    return values.reduce((total, value) => total + 1, 0);
}

function notFixedByContext() {
    var inferred = function(int value) { // warning
    };
    any untyped = function(int value) { // warning
    };
    function anyFunction = function(int value) { // warning
    };
    _ = [inferred, untyped, anyFunction];
}

type IntFunction function (int a, int b) returns int;

function apply(function (int) returns int f) returns int => f(1);

function applyOptional(IntFunction? f = ()) returns int => f is () ? 0 : f(1, 2);

function applyAll(int seed, IntFunction... functions) returns int => seed + functions.length();

function fixedByParameter(int[] values) returns int {
    int applied = apply(function(int value) returns int => 1);
    int counted = values.reduce(
            function(int total, int value) returns int => total + 1, 0);
    int optional = applyOptional(f = function(int x, int y) returns int => x);
    int all = applyAll(0, function(int x, int y) returns int => x, function(int x, int y) returns int => y);
    return applied + counted + optional + all;
}

function fixedByUnionOrCollection() {
    IntFunction? optional = function(int x, int y) returns int => x;
    (IntFunction|string)[] mixed = [function(int x, int y) returns int => x];
    map<IntFunction> byName = {first: function(int x, int y) returns int => x};
    record {|IntFunction first;|} byField = {first: function(int x, int y) returns int => x};
    [int, int, IntFunction] spread = [...[1, 2], function(int x, int y) returns int => x];
    [int, IntFunction, int, int] between = [...[1], function(int x, int y) returns int => x, ...[3, 4]];
    [int, IntFunction...] rest = [...[1], function(int x, int y) returns int => x];
    [IntFunction, int] [destructured, n] = [function(int x, int y) returns int => x, 1];
    _ = [optional, mixed, byName, byField, spread, between, rest, destructured, n];
}

type Pair record {|
    IntFunction f;
    int n;
|};

function fixedByDestructuring() {
    [[IntFunction, int], int] [[f1, n1], m1] = [[function(int x, int y) returns int => x, 1], 2];
    [int, IntFunction...] [n2, ...rest2] = [1, function(int x, int y) returns int => x];
    [IntFunction, int] [_, n3] = [function(int x, int y) returns int => x, 1]; // warning: `_` has no type
    Pair {f: f4, n: n4} = {f: function(int x, int y) returns int => x, n: 1};
    IntFunction f5;
    int n5;
    [f5, n5] = [function(int x, int y) returns int => x, 1];
    int[1] one = [1];
    [int, IntFunction, int] spreadArrays = [...one, function(int x, int y) returns int => x, ...one];
    _ = [f1, n1, m1, n2, rest2, n3, f4, n4, f5, n5, spreadArrays];
}

type Holder record {|
    IntFunction[] fs;
|};

function fixedByCapturedCollection() {
    [IntFunction[], int] [fs1, n1] = [[function(int x, int y) returns int => x], 1];
    [[IntFunction, int], int] [p2, m2] = [[function(int x, int y) returns int => x, 1], 2];
    Holder {fs: fs3} = {fs: [function(int x, int y) returns int => x]};
    IntFunction[] fs4;
    int n4;
    [fs4, n4] = [[function(int x, int y) returns int => x], 1];
    _ = [fs1, n1, p2, m2, fs3, fs4, n4];
}

function fixedByRestOrQuotedField() {
    IntFunction f1;
    map<IntFunction> others1;
    {f: f1, ...others1} = {f: function(int x, int y) returns int => x, g: function(int x, int y) returns int => y};
    record {|IntFunction f; IntFunction g;|} {f: f2, g: g2} = {"f": function(int x, int y) returns int => x,
        g: function(int x, int y) returns int => y};
    _ = [f1, others1, f2, g2];
}

type FunctionPair [IntFunction, int];

type AnyPair record {|
    any f;
    int n;
|};

function fixedByForEach() {
    foreach FunctionPair [f, n] in [[function(int x, int y) returns int => x, 1]] {
        _ = [f, n];
    }
    foreach IntFunction f in ([function(int x, int y) returns int => x]) {
        _ = f;
    }
    foreach IntFunction f in {first: function(int x, int y) returns int => x} {
        _ = f;
    }
    foreach IntFunction f in ({first: function(int x, int y) returns int => x}) {
        _ = f;
    }
}

function notFixedByForEach() {
    foreach any f in [function(int value) { // warning
    }] {
        _ = f;
    }
    foreach AnyPair {f, n} in [{f: function(int value) { // warning
    }, n: 1}] {
        _ = [f, n];
    }
    foreach [IntFunction, int] [f, n] in [[function(int x, int y) returns int => x, 1]] { // warning: inline type
        _ = [f, n];
    }
    foreach var f in [function(int value) { // warning
    }] {
        _ = f;
    }
}

function fixedAfterSpreadOrComment() {
    [int, int, IntFunction] [a, b, f] = [...[1, 2], function(int x, int y) returns int => x];
    [int, int, IntFunction] [n, ...rest] = [...[1, 2], function(int x, int y) returns int => x];
    record {|IntFunction f;|} commented = {
        // handler
        f: function(int x, int y) returns int => x
    };
    _ = [a, b, f, n, rest, commented];
}

function notFixedByCollection() {
    var functions = [function(int value) { // warning
    }];
    var byName = {first: function(int value) { // warning
    }};
    map<any> anyByName = {first: function(int value) { // warning
    }};
    _ = [functions, byName, anyByName];
}

function fixedByObjectType() {
    Fetcher typedFetcher = isolated client object {
        remote isolated function fetch(string path) returns string|error => error("not supported");

        isolated function close(boolean force) {
        }
    };
    var untypedFetcher = isolated client object {
        remote isolated function fetch(string path) returns string|error => error("not supported"); // warning
    };
    _ = [typedFetcher, untypedFetcher];
}
