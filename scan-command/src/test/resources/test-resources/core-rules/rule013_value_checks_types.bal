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




type Secret string;
type Token "live-key-9f2"|"test-key-7a1";
type MaybeSecret Secret?;
type Flag boolean;
type Port int|float;

type Settings record {|
    string host;
|};

class Holder {
}

const FIXED_VALUE = "hunter22";
const LIVE_KEY = "live-key-9f2";
const int RETRIES = 3;
const boolean ENABLED = true;
const Settings SETTINGS = {host: "localhost"};

public function main() {
    string password = FIXED_VALUE;
    Secret dbPassword = FIXED_VALUE;
    MaybeSecret adminPassword = FIXED_VALUE;
    Token token = LIVE_KEY;
    "live-key-9f2" apiKey = LIVE_KEY;
    json clientSecret = FIXED_VALUE;
    anydata authToken = FIXED_VALUE;
    any accessToken = FIXED_VALUE;
    readonly apiToken = FIXED_VALUE;
    string|error userPassword = FIXED_VALUE;
    string? rootPassword = FIXED_VALUE;
    int|string sessionToken = FIXED_VALUE;
    readonly & string refreshToken = FIXED_VALUE;
    printValue(password, dbPassword, adminPassword, token, apiKey, clientSecret, authToken,
            accessToken, apiToken, userPassword, rootPassword, sessionToken, refreshToken);
    printSkipped();
}

function printSkipped() {
    boolean secret = ENABLED;
    Flag auth = ENABLED;
    int token = RETRIES;
    Port apiKey = RETRIES;
    float password = 1.5;
    decimal pwd = 2.5d;
    Settings secretSettings = SETTINGS;
    Settings authSettings = {host: "localhost"};
    readonly & Settings tokenSettings = SETTINGS;
    Holder secretHolder = new;
    string? apiSecret = ();
    error? tokenError = ();
    int|boolean passwordFlag = false;
    printValue(secret, auth, token, apiKey, password, pwd, secretSettings, authSettings, tokenSettings,
            secretHolder, apiSecret, tokenError, passwordFlag);
}

function printValue(any|error... values) {
    _ = values;
}
