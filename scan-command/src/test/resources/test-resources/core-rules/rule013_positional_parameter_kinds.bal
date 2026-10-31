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



type Config record {|
    string host;
    int port = 8080;
|};

configurable string dbPassword = ?;

public function main() {
    withIncludedRecord("includedSecret1", {host: "localhost"}, "includedToken1");
    withIncludedRecord("includedSecret2", host = "localhost");
    withDefaults("admin", "eu-west", "defaultableSecret1");
    withDefaults("admin", "eu-west");
    withDefaults("defaultableUser", password = "defaultableSecret2");
    twoSecrets("firstSecret1", "secondSecret1", "admin");
    twoSecrets("firstSecret2", token = "secondSecret2", user = "admin");
    restTokens("admin", "restToken1", "restToken2");
    restTokens("admin");
    string[] restValues = readTokens();
    restTokens("admin", ...restValues);
    secretThenRest("leadingSecret1", "label1", "label2");
    onlyRest("onlyRestSecret1", "onlyRestSecret2");
    onlyIncluded({host: "localhost"});
    noParams();
    twoSecrets(password = "namedSecret1", token = dbPassword, user = "admin");
}

function withIncludedRecord(string password, *Config config, string... tokens) {
    printValue(password, config, tokens);
}

function withDefaults(string user, string region = "us-east", string password = "") {
    printValue(user, region, password);
}

function twoSecrets(string password, string token, string user) {
    printValue(password, token, user);
}

function restTokens(string user, string... tokens) {
    printValue(user, tokens);
}

function secretThenRest(string password, string... labels) {
    printValue(password, labels);
}

function onlyRest(string... passwords) {
    printValue(passwords);
}

function onlyIncluded(*Config config) {
    printValue(config);
}

function noParams() {
    printValue("no parameters");
}

function readTokens() returns string[] {
    return [dbPassword];
}

function printValue(any|error... values) {
    _ = values;
}
