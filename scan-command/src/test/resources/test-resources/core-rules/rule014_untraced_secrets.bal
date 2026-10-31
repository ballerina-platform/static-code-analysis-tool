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



configurable string username = ?;
configurable string password = ?;
configurable Credentials configuredCredentials = ?;

type Credentials record {|
    string username;
    string password;
|};

public function main() returns error? {
    Credentials credentials = {username, password};
    connect(password = credentials.password);
    connect(password = configuredCredentials.password);
    connect(password = "Bearer " + password);
    string accessToken = fetchToken();
    map<string> headers = {"Authorization": accessToken};
    byte[] sharedSecret = check decode(accessToken);
    map<string> basicAuth = {"Authorization": "Basic " + string `${username}:${password}`.toBytes().toBase64()};
    printValue(headers, sharedSecret, basicAuth);
}

function login(string pwd) {
    connect(password = pwd);
}

function connect(string password) {
    printValue(password);
}

function fetchToken() returns string {
    return string `token-${username}`;
}

function decode(string value) returns byte[]|error {
    return value.toBytes();
}

function printValue(any|error... values) {
    _ = values;
}
