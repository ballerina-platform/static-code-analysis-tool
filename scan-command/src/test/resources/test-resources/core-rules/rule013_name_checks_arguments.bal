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

function connect(string host, string tokenUrl = "", string password = "", string userName = "",
        string apiKey = "") returns string {
    return host + tokenUrl + password + userName + apiKey;
}

function fetchToken(string clientSecret = "param-default-01", string tokenEndpoint = "https://idp.example.com/t",
        string authToken = string `param-default-02`, string 'secret = "param-default-03",
        string passwordHint = "first pet", string bearer = "${TOKEN}") returns string {
    return clientSecret + tokenEndpoint + authToken + 'secret + passwordHint + bearer;
}

class Vault {
    function login(string userName = "", string passphrase = "", string secretPath = "") returns string {
        return userName + passphrase + secretPath;
    }
}

public function main() {
    string connection = connect(host = "localhost", password = "named-arg-01", userName = "admin");
    string secondConnection = connect(host = "localhost", tokenUrl = "https://idp.example.com/token",
            apiKey = string `named-arg-02`);
    string fetched = fetchToken(tokenEndpoint = "https://idp.example.com/token");
    Vault vault = new;
    string session = vault.login(userName = "admin", passphrase = "named-arg-03",
            secretPath = "/secret/data/app");
    string placeholderConnection = connect(host = "localhost", password = "your-password-here");
}
