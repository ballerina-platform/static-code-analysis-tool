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




type Octet byte;

type Account record {|
    string username;
    string password;
|};

configurable string username = ?;
configurable string dbPassword = ?;
configurable string:Char separator = ?;
configurable byte[] keyBytes = ?;
configurable Octet[] octets = ?;
configurable json jsonSecret = ?;
configurable anydata anySecret = ?;
configurable string? optionalSecret = ();
configurable map<string> tokenMap = ?;
configurable string[] keyList = ?;
configurable Account account = ?;
configurable string password = ?;
configurable string apiKey = "";
configurable string token = "<token>";

string moduleSecret = dbPassword;
final readonly & byte[] secretKey = keyBytes;

public function main() {
    string? clientSecret = optionalSecret;
    string|error accessToken = dbPassword;
    string:Char pwd = separator;
    byte[] signingSecret = keyBytes;
    Octet[] hmacSecret = octets;
    json authToken = jsonSecret;
    anydata apiToken = anySecret;
    map<string> secrets = tokenMap;
    map<string> clientSecrets = {github: dbPassword, slack: username};
    map<string> passwords = {password};
    string[] apiKeys = [dbPassword, password];
    string[] tokens = keyList;
    Account credentialsRecord = {username, password};
    connect(account);
    connect(auth = {username, password: dbPassword});
    connect(auth = account);
    printValue(moduleSecret, secretKey, clientSecret, accessToken, pwd, signingSecret, hmacSecret, authToken,
            apiToken, secrets, clientSecrets, passwords, apiKeys, tokens, credentialsRecord, apiKey, token);
}

function connect(Account auth) {
    printValue(auth);
}

function printValue(any|error... values) {
    _ = values;
}
