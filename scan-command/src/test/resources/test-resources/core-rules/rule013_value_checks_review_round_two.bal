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

const string[] DEFAULT_VALUES = ["a1b2c3d4"];
const byte[] KEY_BYTES = [12, 34, 56];
const int[] TOKEN_RETRY_DELAYS = [1, 2, 5];
const int maxTokens = 3600;
final string[] KEYS = ["PYUcBbQy8DgdZ"];

enum AuthScheme {
    BASIC,
    OAUTH2
}

public function main() {
    string[] apiTokens = DEFAULT_VALUES;
    byte[] secretKey = KEY_BYTES;
    string dsn = "primary=postgres://u:${PG_PASS}@h1 replica=postgres://u:r3alP4ss@h2";
    string[] githubTokens = KEYS;
    int[] tokenDelays = TOKEN_RETRY_DELAYS;
    string auth = OAUTH2;
    record {|int maxTokens;|} limits = {maxTokens};
    printValue(apiTokens, secretKey, dsn, githubTokens, tokenDelays, auth, limits);
}

function printValue(any... values) {
    _ = values;
}
