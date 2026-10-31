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




public function main() returns error? {
    Vault vault = new;
    vault.rotate();
    vault.unlock("admin", "methodSecret1");

    AuthClient authClient = new;
    authClient->login("admin", "remoteSecret1");
    authClient->/users/["42"]/login.post("admin", "resourceSecret1");
    authClient->/users/["42"]/login.post("admin", password = "resourceSecret2");
    authClient->/tokens/["42"].put("resourceToken1");

    future<()> started = start connect("admin", "startSecret1");
    check wait started;
    future<()> startedRemote = start authClient->login("admin", "startSecret2");
    check wait startedRemote;

    function () lambda = function() {
        connect("admin", "lambdaSecret1");
    };
    lambda();
    function (string) returns string arrow = input => sign(input, "arrowSecret1");
    printValue(arrow("data"));

    Session checked = check new ("admin", "newSecret1");
    Session checkedExplicit = check new Session("admin", "newSecret2");
    Session|error union = new ("admin", "newSecret3");
    Session|error unionExplicit = new Session("admin", "newSecret4");
    printValue(checked, checkedExplicit, union, unionExplicit);
    NoInit noInit = new;
    NoInit noInitExplicit = new NoInit();
    printValue(noInit, noInitExplicit);

    Authenticator authenticator = vault;
    authenticator.unlock("admin", "objectTypeSecret1");
    future<()> unlocking = start authenticator.unlock("admin", "startSecret3");
    check wait unlocking;
    function (string, string) unnamed = connect;
    unnamed("admin", "unnamedPointerValue1");
}

function connect(string user, string password) {
    printValue(user, password);
}

function sign(string input, string secret) returns string {
    return input + secret;
}

type Authenticator object {
    isolated function unlock(string user, string secret);
};

class Vault {
    isolated function unlock(string user, string secret) {
        printValue(user, secret);
    }

    function rotate() {
        self.unlock("admin", "selfSecret1");
    }
}

client class AuthClient {
    remote isolated function login(string user, string password) {
        printValue(user, password);
    }

    resource function post users/[string id]/login(string user, string password) {
        printValue(id, user, password);
    }

    resource function put tokens/[string id](string token) {
        printValue(id, token);
    }
}

class Session {
    function init(string user, string password) returns error? {
        printValue(user, password);
    }
}

class NoInit {
    function describe() returns string {
        return "no init";
    }
}

isolated function printValue(any|error... values) {
    _ = values;
}
