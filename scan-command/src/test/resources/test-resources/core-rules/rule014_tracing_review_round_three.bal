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

configurable string emptyToken = "";
string base = "base-value";
configurable string fromVariable = base;
const map<string> CREDS = {password: "const-map-secret"};
public final map<string> sharedSettings = {db: "public-value"};

enum AuthScheme {
    BASIC,
    OAUTH2
}

public function main() {
    map<string> headers = {"Authorization": "Bearer " + emptyToken};
    string authorization = string `Bearer ${emptyToken}`;
    connect(password = fromVariable);
    connect(password = CREDS.password);
    map<string> changed = {db: "dev-value"};
    changed["db"] = fetch();
    connect(password = changed["db"]);
    map<string> updated = {db: "dev-value"};
    _ = updated.remove("db");
    connect(password = updated["db"] ?: "");
    map<string> loaded = {db: "dev-value"};
    load(loaded);
    connect(password = loaded["db"]);
    connect(password = sharedSettings["db"]);
    string key = "hunter2-key";
    map<int> lookup = {};
    lookup[key] = 1;
    connect(password = key);
    connect(password = OAUTH2);
    printValue(headers, authorization);
}

function connect(string? password) {
    printValue(password);
}

function load(map<string> values) {
    values["db"] = fetch();
}

function fetch() returns string => "runtime";

function printValue(any|error... values) {
    _ = values;
}
