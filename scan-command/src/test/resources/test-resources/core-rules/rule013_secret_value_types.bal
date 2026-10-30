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



type Octet byte;

const API_KEY = "changeme";
const password = "s3cr3t-constant";

string fixedValue = "fixed-secret";
Octet[] fixedBytes = [1, 2, 3];
"live-key"|"test-key" environmentValue = "live-key";
final readonly & byte[] frozenBytes = [4, 5, 6];

public function main() {
    string[] apiTokens = ["ghp_live1", "ghp_live2"];
    map<string> tokens = {github: "ghp_x", slack: "xoxb-y"};
    anydata apiKey = "anydata-secret";
    json clientSecret = fixedValue;
    string|error accessToken = fixedValue;
    Octet[] secretKey = fixedBytes;
    connect(apiKey = API_KEY);
    record {|string password;|} credentialRecord = {password};
    string dbUrl = "postgres://admin:${DB_PASSWORD}@db:5432/app";
    string otherUrl = "https://user:changeme@host";
    string realUrl = "postgres://admin:hunter22@db/app";
    string slack = "xoxb-z";
    map<string> webhookTokens = {slack};
    string? apiSecret = environmentValue;
    byte[] secretBytes = frozenBytes;
    printValue(apiTokens, tokens, apiKey, clientSecret, accessToken, secretKey, credentialRecord, dbUrl, otherUrl,
            realUrl, webhookTokens, apiSecret, secretBytes);
}

function connect(string apiKey) {
    printValue(apiKey);
}

function printValue(any|error... values) {
    _ = values;
}
