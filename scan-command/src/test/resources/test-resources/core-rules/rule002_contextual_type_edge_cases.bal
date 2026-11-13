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

type IntFunction function (int a, int b) returns int;

type Pair record {|
    IntFunction f;
    int n;
|};

[IntFunction, int] [moduleFunction, moduleCount] = [function(int x, int y) returns int => x, 1];

class Holder {
    IntFunction f;

    function init(IntFunction f) {
        self.f = f;
    }

    function apply(IntFunction f) returns int => f(1, 2);
}

type TwoFunctions record {|
    IntFunction f;
    IntFunction g;
|};

type FunctionAndCount [IntFunction, int];

type Escaped record {|
    IntFunction 'my\-fn;
|};

class Fallible {
    function init(IntFunction f) returns error? {
        _ = f;
    }
}

function fixedByDeclaredType() returns error? {
    TwoFunctions {f: onlyF} = {f: function(int x, int y) returns int => x, g: function(int x, int y) returns int => y};
    FunctionAndCount [_, count] = [function(int x, int y) returns int => x, 1];
    Escaped escaped = {'my\-fn: function(int x, int y) returns int => x};
    Fallible implicit = check new (function(int x, int y) returns int => x);
    Fallible explicit = check new Fallible(function(int x, int y) returns int => x);
    _ = [onlyF, count, escaped, implicit, explicit];
}

function applyFrom(IntFunction 'from) returns int => 'from(1, 2);

function fixedByQuotedArgumentOrWildcard() {
    int applied = applyFrom('from = function(int x, int y) returns int => x);
    IntFunction _ = function(int x, int y) returns int => x;
    _ = applied;
}

class Runner {
    function (IntFunction f) returns int run;

    function init(function (IntFunction f) returns int run) {
        self.run = run;
    }

    function go() returns int => self.run(function(int x, int y) returns int => x);
}

function fixedByShorthandFieldPattern() {
    Pair {n, f: g} = {n: 1, f: function(int x, int y) returns int => x};
    _ = [n, g];
}

function fixedByCall(function (IntFunction f) returns int callback) returns int {
    Holder explicit = new Holder(function(int x, int y) returns int => x);
    Holder implicit = new (function(int x, int y) returns int => x);
    int applied = explicit.apply(function(int x, int y) returns int => x);
    function (IntFunction f) returns int local = callback;
    int fromVariable = local(function(int x, int y) returns int => x);
    int fromParameter = callback(function(int x, int y) returns int => x);
    _ = [implicit, moduleFunction, moduleCount];
    return applied + fromVariable + fromParameter;
}

function fixedByPatternOrSpread(int[] ints, IntFunction[] functions) {
    Pair {f, n} = {f: function(int x, int y) returns int => x, n: 1};
    record {|IntFunction 'from;|} quoted = {'from: function(int x, int y) returns int => x};
    [int, IntFunction, IntFunction...] [first, ...rest] = [1, function(int x, int y) returns int => x,
        function(int x, int y) returns int => y];
    [int, IntFunction...] afterSpread = [1, ...functions, function(int x, int y) returns int => x];
    [int, IntFunction...] [m, ...restAfterSpread] = [1, ...functions, function(int x, int y) returns int => x];
    Pair {n: count, f: g} = {n: 1, f: function(int x, int y) returns int => x};
    record {|IntFunction f; IntFunction g;|} {f: unbound} = {f: function(int x, int y) returns int => x,
        g: function(int x, int y) returns int => y}; // warning: inline type
    [IntFunction, IntFunction...] functionTuple = [function(int x, int y) returns int => x];
    [int, IntFunction...] afterTupleSpread = [1, ...functionTuple, function(int x, int y) returns int => x];
    IntFunction[] assigned;
    assigned = [function(int x, int y) returns int => x];
    _ = [count, g, unbound, afterTupleSpread];
    _ = [f, n, quoted, first, rest, afterSpread, m, restAfterSpread, assigned, ints];
}

function notFixedByContext(IntFunction[] functions) {
    any list = [function(int value) { // warning
    }];
    any mapping = {f: function(int value) { // warning
    }};
    _ = [list, mapping, functions];
}
