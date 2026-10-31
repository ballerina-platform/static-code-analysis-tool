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




const API_KEY = "${API_KEY}";
const secret = "<secret>";
const PLACEHOLDER_KEY = "<api-key>";
const EMPTY_VALUE = "";
const CHANGE_ME = "changeme";
const MASKED = "********";
const YOUR_TOKEN = "your-access-token-here";

string password = CHANGE_ME;
string apiKey = API_KEY;

type Account record {|
    string username;
    string password = EMPTY_VALUE;
    string token = "%AUTH_TOKEN%";
|};

class Vault {
    string secret = PLACEHOLDER_KEY;
    string apiKey = "{{vault_key}}";

    function show() {
        printValue(self.secret, self.apiKey);
    }
}

public function main() {
    string token = MASKED;
    string? clientSecret = YOUR_TOKEN;
    string accessToken = "$ACCESS_TOKEN";
    string[] apiKeys = [EMPTY_VALUE, PLACEHOLDER_KEY, "REDACTED", "  "];
    map<string> secrets = {github: CHANGE_ME, slack: "TODO"};
    map<string> vaultSecrets = {secret};
    Account account = {username: "admin", password: PLACEHOLDER_KEY, token: API_KEY};
    connect("db", CHANGE_ME);
    connect("db", password = MASKED);
    connectWithDefaults("db");
    new Vault().show();
    printValue(password, apiKey, token, clientSecret, accessToken, apiKeys, secrets, vaultSecrets, account);

    string dbPassword = "$ecureP4ss";
    string adminPassword = "<p@ss";
    string rootPassword = "{{oops";
    string userPassword = "your-pass-1234";
    string backupPassword = "Changeme!42";
    printValue(dbPassword, adminPassword, rootPassword, userPassword, backupPassword);
}

function connect(string host, string password) {
    printValue(host, password);
}

function connectWithDefaults(string host, string password = CHANGE_ME, string token = "<token>") {
    printValue(host, password, token);
}

function printValue(any|error... values) {
    _ = values;
}
