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

string fixedValue = "fixed-secret";
string initValue = "";

function init() {
    initValue = fetch();
}

public function main() {
    apply(connect);
    boolean prod = fetch() == "prod";
    string? maybe = prod ? fetch() : ();
    connect("admin", prod ? fetch() : "conditional-secret");
    connect("admin", maybe ?: "elvis-secret");
    connect("admin", prod ? fixedValue : "both-branches-secret");
    connect(prod ? "user-one" : "user-two", "plain-secret");
    string password = "declared-secret";
    password = "reassigned-secret";
    connect("admin", password);
    string late;
    late = fetch();
    connect("admin", late);
    connect("admin", initValue);
    string local = "local-secret";
    connect("admin", local);
}

function apply(function (string user, string password) callback) {
    callback("admin", "parameter-pointer-secret");
}

function connect(string user, string password) {
    printValue(user, password);
}

function fetch() returns string {
    return "value-from-elsewhere";
}

function printValue(any|error... values) {
    _ = values;
}
