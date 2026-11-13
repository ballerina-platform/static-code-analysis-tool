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


import secret_tracing.vault;

string sharedValue = "shared-secret";
string stableValue = "stable-secret";

public function main() {
    connect(password = sharedValue);
    connect(password = stableValue);
    connect(password = rotationValue);
    connect(password = vault:finalValue);
    connect(password = vault:mutableValue);
    connect(password = vault:SHARED_CONSTANT);
    connectAll(apiKeys = vault:API_KEYS);
    connectMap(passwords = vault:DB_PASSWORDS);
    connectMap(passwords = vault:FIELD_PASSWORDS);
    connectNested(passwords = vault:NESTED_PASSWORDS);
    connectPorts(passwords = vault:PORTS);
    sign(secretKey = vault:KEY_BYTES);
    connectMap(passwords = vault:DEFAULT_CREDS);
}

function sign(byte[] secretKey) {
    printValue(secretKey);
}

function connectAll(string[] apiKeys) {
    printValue(apiKeys);
}

function connectMap(map<string> passwords) {
    printValue(passwords);
}

function connectNested(map<map<string>> passwords) {
    printValue(passwords);
}

function connectPorts(map<int> passwords) {
    printValue(passwords);
}

function connect(string password) {
    printValue(password);
}

function printValue(any|error... values) {
    _ = values;
}
