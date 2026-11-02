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

string dbPassword = "mod-var-01";
string GITHUB_TOKEN = "mod-var-02";
string 'apiKey = "mod-var-03";
string tokenForPaymentProvider = "mod-var-04";
string tokenUrl = "https://idp.example.com/oauth2/token";
string passwordFilePath = "/run/secrets/db-password";
string author = "Jane Doe";
string secretary = "John Doe";
string clientSecretPlaceholder = "s3cr3t-mod";
configurable string apiSecret = ?;

const JWT_SECRET = "const-01";
const string PRIVATE_KEY = "const-02";
const SSO_AUTH = "const-03";
const TOKEN_TYPE = "Bearer";
const AUTH_HEADER_NAME = "Authorization";
const OAUTH = "oauth2";
const PASSWD = "changeme";

public function main() {
    string accesstoken = "local-var-01";
    string db\-password = "local-var-02";
    string JWTToken = "local-var-03";
    string passwordForFileServer = "local-var-04";
    string authToken = string `local-var-05`;
    string tokenizer = "whitespace";
    string tokenTypeHint = "refresh_token";
    string secretKeyName = "signing-key";
    string accessKeyId = "AKIAIOSFODNN7EXAMPLE";
    string bypassCode = "bypass-01";
    string myOAuth = "enabled";
    string tokenUrlForProvider = "https://idp.example.com/token";
    string secret = "<secret>";
    string passphrase = string `{{passphrase}}`;
}
