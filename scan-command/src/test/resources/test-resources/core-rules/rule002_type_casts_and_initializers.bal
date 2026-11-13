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

type F function (int a, int b) returns int;

const annotation Tag;

function castForEachSource() returns int {
    int total = 0;
    foreach F f in <F[]>[function(int a, int b) returns int => a] {
        total += f(1, 2);
    }
    foreach var f in <F[]>[function(int a, int b) returns int => a] {
        total += f(1, 2);
    }
    F[] declared = <F[]>[function(int a, int b) returns int => a];
    foreach F f in declared {
        total += f(1, 2);
    }
    return total;
}

function castOperands() {
    any anyFunction = <F>function(int a, int b) returns int => a;
    F annotated = <@Tag>function(int a, int b) returns int => a;
    _ = [anyFunction, annotated];
}

function castToUntyped() {
    any toAny = <any>function(int a, int b) returns int => a; // warning
    var annotatedUntyped = <@Tag>function(int a, int b) returns int => a; // warning
    _ = [toAny, annotatedUntyped];
}

class Holder {
    F f = function(int a, int b) returns int => a;
    any anyFunction = function(int a, int b) returns int => a; // warning
}

type WithDefault record {|
    F f = function(int a, int b) returns int => a;
|};

function withDefault(F f = function(int a, int b) returns int => a) returns int => f(1, 2);

function withLet() returns int => let F f = function(int a, int b) returns int => a,
        var untyped = function(int a, int b) returns int => a in f(1, 2) + untyped(1, 2); // warning

function annotatedForEachSource() returns int {
    int total = 0;
    foreach F f in <@Tag>{first: function(int a, int b) returns int => a} {
        total += f(1, 2);
    }
    foreach F f in (<@Tag>[function(int a, int b) returns int => a]) {
        total += f(1, 2);
    }
    return total;
}
