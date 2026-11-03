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

type Settings record {|
    string dbValue;
|};

string apiKey = "";
final Settings settings = {dbValue: "dev-value"};

class Client {
    string apiKey;

    function init() {
        self.apiKey = "sk_live_object";
    }
}

function init() {
    apiKey = "sk_live_module";
    settings.dbValue = fetch();
}

public function main() {
    connect(password = "a1" + "b2" + "c3" + "d4" + "e5" + "f6" + "g7");
    connect(password = "admin" + ":" + "s3cr3t");
    string user = "admin";
    string pass = "fixed-pass";
    connect(password = user + ":" + pass);
    string password;
    password = "assigned-secret";
    connect(password = password);
    map<string> headers = {};
    headers["Authorization"] = "Bearer abc123";
    connect(password = settings.dbValue);
    var holder = {dbValue: "rec-secret"};
    connect(password = holder.dbValue);
    string raw = "raw-secret";
    byte[] secretKey = raw.toBytes();
    boolean prod = fetch() == "prod";
    string token = prod ? fetch() : "dev-token-123";
    map<string> values = {db: "map-secret"};
    connect(password = values["db"]);
    Client c = new;
    printValue(apiKey, headers, secretKey, token, c);
}

function connect(string? password) {
    printValue(password);
}

function fetch() returns string {
    return "value-from-elsewhere";
}

function printValue(any|error... values) {
    _ = values;
}
