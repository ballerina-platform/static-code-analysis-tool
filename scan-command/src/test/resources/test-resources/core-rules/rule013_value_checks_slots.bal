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




const FIXED_VALUE = "hunter22";

configurable string dbPassword = ?;

string apiKey = "module-key-123";
final string secret = FIXED_VALUE;
string token = dbPassword;

type Account record {|
    string username;
    string password = "rec-default-1";
    string authToken = FIXED_VALUE;
    boolean auth = true;
|};

class Vault {
    string secret = "obj-secret-1";
    string apiKey = FIXED_VALUE;
    string? password = ();
    int token = 5;

    function show() {
        printValue(self.secret, self.apiKey, self.password, self.token);
    }
}

public function main() {
    string clientSecret = "local-secret-1";
    string accessToken = FIXED_VALUE;
    string password = dbPassword;
    connect("db", "positional-1");
    connect("db", FIXED_VALUE);
    connect("db", dbPassword);
    connect(host = "db", password = "named-pass-1");
    connect(host = "db", password = FIXED_VALUE);
    connect(host = "db", password = dbPassword);
    connectWithDefaults("db");
    Account account = {username: "admin", password: "field-pass-1", authToken: dbPassword};
    new Vault().show();
    printValue(apiKey, secret, token, clientSecret, accessToken, password, account);
}

function connect(string host, string password) {
    printValue(host, password);
}

function connectWithDefaults(string host, string password = "default-pass-1", string token = FIXED_VALUE,
        string apiKey = dbPassword) {
    printValue(host, password, token, apiKey);
}

function printValue(any|error... values) {
    _ = values;
}
