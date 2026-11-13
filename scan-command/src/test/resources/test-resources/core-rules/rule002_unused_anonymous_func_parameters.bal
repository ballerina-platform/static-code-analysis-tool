// Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
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

// The parameters of an anonymous function used where a function type is expected are fixed by that type,
// so they are not reported even when unused.

function testExprFunctions(int a, int b, int c) { // warning * 2
   [1,2].forEach(element => ());
   [1,2].forEach(element => doNothing(element + c));
}

function (int, int) returns int anonFunc1 = (x, y) => x + y;

function (int, int) returns int anonFunc2 = function (int x, int y) returns int => x + y;

function anonFunc3(int a) => [1,2].forEach(element => doNothing(element)); // warning

function anonFunc4(int a) => [1,2].forEach(element => doNothing(a));

function (int, int) returns int anonFunc5 = (x, y) => x;

function (int, int) returns int anonFunc6 = function (int x, int y) returns int => x;

function anonFunc7(int a, int b) => [1,2].forEach(element => doNothing(b)); // warning

type F function (int, int) returns int;

type R record {
    F f = (a, b) => a;
    F anotherF = function(int a, int b) returns int {
        return b;
    };
};

function testInlineFunctionDecl() {
    F[] _ = [
        (a, b) => a,
        function(int a, int b) returns int {
            return b;
        }
    ];
}

function main(int a, int b, int c) { // warning
    _ = doNothing(a);
    _ = doNothing(c);
    [1,2].forEach((element) => ());
    [1,2].forEach((element) => doNothing(element));
}

function doNothing(any a) { // warning
    return;
}

function (int a , int b) returns int v = (x, y) => x + y;

function (int a , int b) returns int v2 = (x, y) => x;
