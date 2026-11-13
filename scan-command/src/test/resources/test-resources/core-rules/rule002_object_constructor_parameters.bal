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

// Methods of object constructors, which are reported unless the type of the constructor declares them, including
// constructors that initialize fields, default values and let variables.

type Op object {
    function m(int p) returns int;
};

Op typedModuleObject = object {
    function m(int p) returns int => 1;
};

var untypedModuleObject = object {
    function m(int p) returns int => 1; // warning
};

function untypedObjects() returns int {
    var untyped = object {
        function m(int p) returns int { // warning
            return 1;
        }
    };
    object {} emptyType = object {
        function m(int p) returns int => 1; // warning
    };
    var fetcher = isolated client object {
        remote isolated function fetch(string path) returns string|error => error("not supported"); // warning
    };
    var used = object {
        function m(int p) returns int => p;
    };
    return untyped.m(1) + used.m(1);
}

function capturedByObject(int captured, int unused) returns int { // warning
    var o = object {
        function m(int p) returns int => captured; // warning
    };
    return o.m(1);
}

function typedObject() returns int {
    Op typed = object {
        function m(int p) returns int => 1;
    };
    return typed.m(1);
}

function plain(int p) returns int { // warning
    return 1;
}

type Named object {
    function name() returns string;
};

class Holder {
    final Op finalField = object {
        function m(int p) returns int => 1;
    };
    Op plainField = object {
        function m(int p) returns int => 1;
    };
    Named namedField = object {
        function name() returns string => "named";

        function m(int p) returns int => 1; // warning
    };
    object {} emptyTypeField = object {
        function m(int p) returns int => 1; // warning
    };
}

type WithDefaults record {|
    Op op = object {
        function m(int p) returns int => 1;
    };
    object {} emptyTypeValue = object {
        function m(int p) returns int => 1; // warning
    };
|};

function withDefault(Op op = object {
            function m(int p) returns int => 1;
        }, object {} emptyTypeValue = object {
            function m(int p) returns int => 1; // warning
        }) returns int {
    _ = emptyTypeValue;
    return op.m(1);
}

function withLet() returns int => let Op op = object {
            function m(int p) returns int => 1;
        }, var untyped = object {
            function m(int p) returns int => 1; // warning
        } in op.m(1) + untyped.m(1);

function returned() returns Op {
    return object {
        function m(int p) returns int => 1;
    };
}

function returnedByExpression() returns Op => object {
    function m(int p) returns int => 1;
};

function assigned() returns int {
    Op op;
    op = object {
        function m(int p) returns int => 1;
    };
    return op.m(1);
}
