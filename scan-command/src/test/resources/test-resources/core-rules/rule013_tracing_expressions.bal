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



const BASE_VALUE = "base-secret";
const DERIVED_VALUE = BASE_VALUE + "-derived";
const PLACEHOLDER_VALUE = "changeme";
const DERIVED_PLACEHOLDER = PLACEHOLDER_VALUE + "";
const MASTER_PASSWORD = "master-secret";
const DERIVED_MASTER = MASTER_PASSWORD + "-v2";

string fixedValue = "fixed-secret";
configurable string configuredValue = ?;

public function main() {
    connect(password = "first-part" + "second-part");
    connect(password = "prefix-" + fixedValue);
    connect(password = "prefix-" + configuredValue);
    connect(password = fixedValue + configuredValue);
    connect(password = "prefix-" + BASE_VALUE);
    connect(password = ("prefix-" + fixedValue) + "-suffix");
    connect(password = "prefix-" + fetch());
    connect(password = string `${fixedValue}`);
    connect(password = string `${BASE_VALUE}${DERIVED_VALUE}`);
    connect(password = string `${configuredValue}`);
    connect(password = string `${fixedValue}${configuredValue}`);
    connect(password = string `prefix-${fixedValue}`);
    connect(password = string `${"interpolated-secret"}`);
    connect(password = string `${PLACEHOLDER_VALUE}`);
    connect(password = string `changeme`);
    connect(password = DERIVED_VALUE);
    connect(password = DERIVED_PLACEHOLDER);
    connect(password = MASTER_PASSWORD);
    connect(password = DERIVED_MASTER);
    string viaConstant = DERIVED_VALUE;
    connect(password = viaConstant);
    connectBytes(secret = "literal-secret".toBytes());
    connect(password = "literal-secret".toBytes().toBase64());
    connect(password = "literal-secret".toBytes().toBase16());
    connect(password = BASE_VALUE.trim());
    connect(password = " padded-secret ".trim().toUpperAscii());
    connectBytes(secret = base64 `c2VjcmV0LXZhbHVl`);
    connectBytes(secret = [1, 2, 3, 4]);
    byte[] literalBytes = "literal-secret".toBytes();
    connectBytes(secret = literalBytes);
    byte[] numbers = [1, 2, 3, 4];
    connectBytes(secret = numbers);
}

function connect(string password) {
    printValue(password);
}

function connectBytes(byte[] secret) {
    printValue(secret);
}

function fetch() returns string => "runtime";

function printValue(any|error... values) {
    _ = values;
}
