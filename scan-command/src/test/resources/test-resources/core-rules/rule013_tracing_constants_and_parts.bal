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

type Holder record {|
    string value;
|};

enum Keys {
    MASTER_PASSWORD = "enum-master-secret"
}

const AUTHORIZATION_HEADER = "Authorization";
const ACCESS_TOKEN_KEY = "access_token";
const BEARER = "Bearer ";
const SEP = ":";
const EMPTY = "";
const USER = "admin";
const PASS_PART = "s3cr3tValue";
const Holder DEFAULT_HOLDER = {value: "const-record-secret"};
const map<string> DEFAULT_MAP = {db: "const-map-secret"};

configurable map<string> configuredTokens = {github: "configured-default"};
string host = "db.example.com";

class Trimmer {
    function trim() returns string {
        return "not-a-conversion";
    }
}

public function main() {
    connect(password = BEARER + "abc123token");
    connect(password = USER + SEP + PASS_PART);
    connect(password = PASS_PART + EMPTY);
    connect(password = "" + "");
    connect(password = "${DB}" + "${PW}");
    connect(password = DEFAULT_HOLDER.value);
    connect(password = DEFAULT_MAP["db"]);
    connect(password = <string>"cast-secret");
    connect(password = string:trim(" trimmed-secret "));
    connect(password = MASTER_PASSWORD);
    connect(password = "postgres://u:pw1@" + host);
    connect(password = configuredTokens["github"] ?: "");
    Trimmer trimmer = new;
    connect(password = trimmer.trim());
    string password = "first-secret";
    password += "-appended-secret";
    map<map<string>> nested = {db: {value: host}};
    connect(password = nested["db"]["value"] ?: "");
    printValue(AUTHORIZATION_HEADER, ACCESS_TOKEN_KEY, password);
}

function connect(string? password) {
    printValue(password);
}

function printValue(any|error... values) {
    _ = values;
}
