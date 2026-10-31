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



const PREFIX = "s3cr3t";
const SUFFIX = "-value";

public function main() {
    string password = "s3cr3t-value";
    connect(password = password);
    connect(password = "s3cr3t" + "-value");
    connect(password = PREFIX + SUFFIX);
    connect(password = string `s3cr3t-value`);
    byte[] secretKey = "key-material".toBytes();
    byte[] privateKey = base16 `aabbccddeeff`;
    byte[] signingSecret = [1, 2, 3, 4];
    printValue(secretKey, privateKey, signingSecret);
}

function connect(string password) {
    printValue(password);
}

function printValue(any|error... values) {
    _ = values;
}
