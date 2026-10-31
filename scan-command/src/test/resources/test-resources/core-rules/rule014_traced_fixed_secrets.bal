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



configurable string dbConfig = "fallback-secret";
string defaultValue = "default-secret";
final string finalValue = "final-secret";
public string publicValue = "public-secret";
string reassignedValue = "first-secret";

type Credentials record {|
    string username;
    string password;
|};

public function main() {
    connect(password = dbConfig);
    connect(password = defaultValue);
    connect(password = finalValue);
    connect(password = publicValue);
    reassignedValue = "second-secret";
    connect(password = reassignedValue);
    string localValue = defaultValue;
    connect(password = localValue);
    Credentials credentials = {username: "admin", password: defaultValue};
    connect(password = credentials.password);
    connect(password = finalValue + defaultValue);
}

function connect(string password) {
    printValue(password);
}

function printValue(any|error... values) {
    _ = values;
}
