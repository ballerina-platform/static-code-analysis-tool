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




configurable string dbPassword = ?;

public function main() {
    twoSecrets(dbPassword, dbPassword, "admin");
    twoSecrets(readSecret(), readSecret().trim(), "admin");
    twoSecrets("<password>", "", "admin");
    forward(dbPassword);
    optionalSecret(());
    anySecret(true);
    anySecret(42);
    anySecret(3.14);
    anySecret(());
    connect("admin", "notASecret");
}

function twoSecrets(string password, string token, string user) {
    printValue(password, token, user);
}

function forward(string password) {
    twoSecrets(password, password, "admin");
}

function optionalSecret(string? secret) {
    printValue(secret);
}

function anySecret(anydata secret) {
    printValue(secret);
}

function connect(string user, string region) {
    printValue(user, region);
}

function readSecret() returns string {
    return dbPassword;
}

function printValue(any|error... values) {
    _ = values;
}
