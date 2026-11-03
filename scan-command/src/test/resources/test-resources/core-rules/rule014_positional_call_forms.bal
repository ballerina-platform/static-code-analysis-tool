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


import ballerina/crypto;


configurable string configuredValue = ?;

final string fixedValue = "fixedValue";
string moduleValue = "moduleValue";

public function main() returns error? {
    string localValue = "localValue";
    connect("admin", fixedValue);
    connect("admin", localValue);
    connect("admin", moduleValue);
    restTokens("admin", fixedValue, localValue);
    Vault vault = new;
    vault.rotate();
    AuthClient authClient = new;
    authClient->login("admin", fixedValue);
    authClient->/users/["42"]/login.post("admin", fixedValue);
    Session checked = check new ("admin", fixedValue);
    Session|error union = new Session("admin", localValue);
    printValue(checked, union);
    future<()> started = start connect("admin", fixedValue);
    check wait started;
    function () lambda = function() {
        connect("admin", fixedValue);
    };
    lambda();
    string hashed = check crypto:hashBcrypt(localValue);
    printValue(hashed);

    string password = "declaredSecret";
    connect("admin", password);
    string reassigned = "initialValue";
    reassigned = readValue();
    connect("admin", reassigned);
    connect("admin", configuredValue);
    connect("admin", readValue());
    connect(fixedValue, configuredValue);
    connect(user = fixedValue, password = configuredValue);
}

function connect(string user, string password) {
    printValue(user, password);
}

function restTokens(string user, string... tokens) {
    printValue(user, tokens);
}

function readValue() returns string {
    return configuredValue;
}

class Vault {
    isolated function unlock(string user, string secret) {
        printValue(user, secret);
    }

    function rotate() {
        self.unlock("admin", fixedValue);
    }
}

client class AuthClient {
    remote isolated function login(string user, string password) {
        printValue(user, password);
    }

    resource function post users/[string id]/login(string user, string password) {
        printValue(id, user, password);
    }
}

class Session {
    function init(string user, string password) returns error? {
        printValue(user, password);
    }
}

isolated function printValue(any|error... values) {
    _ = values;
}
