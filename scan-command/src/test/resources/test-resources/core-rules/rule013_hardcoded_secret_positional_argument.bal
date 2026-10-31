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


type KeyConfig record {|
    string path;
    string password;
|};

configurable KeyConfig keyConfig = ?;

public function main() returns error? {
    crypto:PrivateKey _ = check crypto:decodeRsaPrivateKeyFromKeyFile("key.pem", "keyPassword1");
    LoginClient loginClient = new;
    loginClient->login("admin", "remotePassword1");
    storeTokens("tokenOne", "tokenTwo");
    ExplicitClient _ = new ExplicitClient("admin", "explicitPassword1");
    ResourceClient resourceClient = new;
    resourceClient->/login.post("admin", "resourcePassword1");
    ReadOnlyClient _ = new ("admin", "readonlyPassword1");
    crypto:PrivateKey _ = check crypto:decodeRsaPrivateKeyFromKeyFile(keyConfig.path, keyConfig.password);
    crypto:PrivateKey _ = check crypto:decodeRsaPrivateKeyFromKeyFile("key.pem", ());
    getBooks("Tolkien");
}

client class ResourceClient {
    resource function post login(string username, string password) {
        printValue(username, password);
    }
}

readonly class ReadOnlyClient {
    function init(string username, string password) {
        printValue(username, password);
    }
}

function getBooks(string author) {
    printValue(author);
}

client class LoginClient {
    remote function login(string username, string password) {
        printValue(username, password);
    }
}

class ExplicitClient {
    function init(string username, string password) {
        printValue(username, password);
    }
}

function storeTokens(string... tokens) {
    printValue(tokens);
}

function printValue(any|error... values) {
    _ = values;
}
