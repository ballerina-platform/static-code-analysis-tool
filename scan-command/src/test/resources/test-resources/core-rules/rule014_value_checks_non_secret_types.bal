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




type Flag boolean;

type Settings record {|
    string host;
|};

class Holder {
}

configurable Settings settings = ?;
configurable boolean enabled = false;

const int MAX_TRIES = 3;
const boolean ENABLED = true;
int[] codes = [1, 2];

public function main() {
    boolean flag = enabled;
    int count = 3;
    float ratio = 0.5;
    Settings localSettings = {host: "localhost"};
    Holder holder = new;
    boolean secret = flag;
    Flag auth = flag;
    int token = count;
    float apiKey = ratio;
    int|float|boolean password = count;
    Settings secretSettings = localSettings;
    Settings tokenSettings = settings;
    readonly & Settings authSettings = {host: "localhost"};
    Holder secretHolder = holder;
    () apiSecret = ();
    int secretTries = MAX_TRIES;
    boolean bearer = ENABLED;
    int[] secretCodes = codes;
    configureFlags(ENABLED, MAX_TRIES);
    configureFlags(auth = ENABLED, token = MAX_TRIES);
    configureFlags(flag, count);
    configureFlags(auth = flag, token = count);
    printValue(secret, auth, token, apiKey, password, secretSettings, tokenSettings, authSettings,
            secretHolder, apiSecret, secretTries, bearer, secretCodes);
}

function configureFlags(boolean auth = true, int token = 0, Settings secret = {host: "localhost"},
        Holder password = new) {
    printValue(auth, token, secret, password);
}

function printValue(any|error... values) {
    _ = values;
}
