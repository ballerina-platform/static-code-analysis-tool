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



type KeyBytes byte[];

const byte[] KEY_BYTES = [1, 2, 3];
const KeyBytes OCTETS = [4, 5, 6];

public function main() {
    byte[] secretKey = KEY_BYTES;
    KeyBytes signingSecret = OCTETS;
    string postgres = "postgresql://admin:pgP4ss@db:5432/app";
    string jdbc = "jdbc:mysql://root:r00tP4ss@db:3306/app";
    string srv = "mongodb+srv://admin:m0ngoP4ss@cluster0.example.net";
    string note = "see https://admin:p4ssw0rd@host for details";
    string apiKey = "\tchangeme";
    string suffix = "-tail";
    string partial = string `https://admin:x9${suffix}@example.com`;
    printValue(secretKey, signingSecret, postgres, jdbc, srv, note, apiKey, partial);
}

function printValue(any|error... values) {
    _ = values;
}
