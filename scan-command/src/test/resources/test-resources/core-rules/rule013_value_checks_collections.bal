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




type Account record {|
    string username;
    string password;
|};

const FIXED_VALUE = "hunter22";

public function main() {
    string[] appSecrets = ["key-one-17", "changeme", FIXED_VALUE, ""];
    string[][] tokens = [["tok-a1", "<token>"], []];
    [string, int] token = ["tuple-secret-1", 3];
    readonly & string[] secretKeys = ["ro-key-123"];
    json[] authTokens = ["json-tok-123", 5, true, ()];
    anydata accessTokens = ["any-tok-123", 1.5];
    map<string> secrets = {github: "ghp_123abc", slack: "<slack-token>", gitlab: FIXED_VALUE};
    map<map<string>> clientSecrets = {github: {live: "ghp_live99", test: "${GH_TEST}"}};
    map<string[]> apiTokens = {svc: ["svc-key-77", "dummy"]};
    map<string>[] passwords = [{admin: "adm-pass-31"}, {}];
    map<Account> accountSecrets = {
        admin: {username: "admin", password: "adm1n-pass"},
        guest: {username: "guest", password: "changeme"}
    };
    json apiSecrets = {github: "json-map-1"};
    string[] emptyTokens = [];
    map<string> emptySecrets = {};
    printValue(appSecrets, tokens, token, secretKeys, authTokens, accessTokens, secrets, clientSecrets, apiTokens,
            passwords, accountSecrets, apiSecrets, emptyTokens, emptySecrets);
}

function printValue(any|error... values) {
    _ = values;
}
