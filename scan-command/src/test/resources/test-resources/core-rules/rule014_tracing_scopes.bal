// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
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


final string defaultValue = "default-secret";

class Client {
    private final string endpointValue = defaultValue;
    private final string password = "class-field-secret";
    private final string clientSecret = defaultValue;

    function call() {
        string methodLocal = "method-local-secret";
        connect(password = methodLocal);
        connect(password = self.endpointValue);
        connect(password = self.password);
    }
}

service /api on new http:Listener(9090) {
    private final string serviceValue = defaultValue;
    private final string apiKey = "service-field-secret";

    resource function get .() {
        connect(password = self.serviceValue);
        connect(password = self.apiKey);
    }
}

public function main() {
    string captured = "captured-secret";
    function () useCaptured = function() {
        connect(password = captured);
    };
    useCaptured();
    string capturedMutated = "captured-mutated-secret";
    function () mutate = function() {
        capturedMutated = fetch();
    };
    mutate();
    connect(password = capturedMutated);
    function () closureLocal = function() {
        string insideClosure = "closure-local-secret";
        connect(password = insideClosure);
    };
    closureLocal();
    connect(password = let string bound = "let-secret" in bound);
    string viaLet = let string bound = defaultValue in bound;
    connect(password = viaLet);
    error? letResult = let string bound = "let-bound-secret" in connect(password = bound);
    from string item in [defaultValue] do {
        connect(password = item);
    };
    from string item in [defaultValue]
        let string derived = item + "-derived"
        do {
            connect(password = derived);
        };
    from string item in [defaultValue] do {
        string doLocal = "do-local-secret";
        connect(password = doLocal + item);
        connect(password = doLocal);
    };
    string[] picked = from string item in ["query-secret"] select item;
    connect(password = picked[0]);
    printValue(letResult);
}

function connect(string password) {
    printValue(password);
}

function fetch() returns string => "runtime";

function printValue(any|error... values) {
    _ = values;
}
