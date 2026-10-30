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
    string dbCredential = "db-credential-1";
    string creds = "user:pass-1";
    string passcode = "482913";
    string privateKey = "private-key-1";
    string accessKey = "access-key-1";
    string clientKey = "client-key-1";
    string bearer = "bearer-token-1";
    string oauthToken = "oauth-token-1";
    string jwtSecret = "jwt-secret-1";
    map<string> headers = {"Authorization": "Bearer abc123", "x-api-key": "api-key-1", "apikey": "api-key-2"};
    string apiSecret = string `hunter2-template`;
    string pass\u{0077}ord = "escaped-name-secret";
}
