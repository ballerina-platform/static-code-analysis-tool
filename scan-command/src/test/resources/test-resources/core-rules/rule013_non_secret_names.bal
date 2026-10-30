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



public function main() {
    string tokenTypeHint = "access_token";
    string tokenUrl = "https://example.com/token";
    string author = "Jane";
    string passwordFilePath = "/etc/app/password.txt";
    string accessKeyId = "AKIAEXAMPLE";
    map<string> config = {tokenEndpoint: "https://example.com/token", "x-token-hint": "refresh_token"};
    map<string> account = {
        // Credentials for the staging database
        username: "admin"
    };
    string token = string `changeme`;
    string password = string ``;
    [string, string] [user, secret] = ["tuple-user", "tuple-secret"];
    string label // password
        = "comment-is-not-the-name";
    printValue(tokenTypeHint, tokenUrl, author, passwordFilePath, accessKeyId, config, account, token, password,
            user, secret, label);
    configure(basicAuthEnabled = true);
}

function configure(boolean basicAuthEnabled = false) {
    printValue(basicAuthEnabled);
}

function printValue(any|error... values) {
    _ = values;
}
