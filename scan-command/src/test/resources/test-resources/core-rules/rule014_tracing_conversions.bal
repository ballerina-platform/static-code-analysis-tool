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
final string finalValue = "final-secret";

public function main() {
    connectBytes(secret = fixedValue.toBytes());
    connect(password = fixedValue.toBytes().toBase64());
    connect(password = fixedValue.toUpperAscii());
    connect(password = finalValue.trim().toLowerAscii());
    string padded = " padded-secret ";
    connectBytes(secret = padded.trim().toBytes());
}

function connect(string password) {
    printValue(password);
}

function connectBytes(byte[] secret) {
    printValue(secret);
}

function printValue(any|error... values) {
    _ = values;
}
