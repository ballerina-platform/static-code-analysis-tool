// Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 Inc. licenses this file to you under the Apache License,
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

import ballerina/io;

string defaultValue = "secret";

public type Credential record {
    string username;
    string password;
};

public type Config record {
    record {|Credential credential;|} nested;
};

public function main() {
    printConfig(nested = {credential: {username: "sabthar", password: defaultValue}});
    PasswordStrengthChecker checker = new;
    checker.checkStregth({username: "sabthar", password: defaultValue});
    Client 'client = new (config = {nested: {credential: {username: "sabthar", password: defaultValue}}});
}

function printConfig(*Config config) {
    io:println(config);
}

class PasswordStrengthChecker {
    function checkStregth(Credential credential) {
        io:println(string `Username: ${credential.username}`,
                string ` Password Strength: ${passwordStrength(credential.password)}%`);
    }
}

function passwordStrength(string word) returns float {
    return <float>word.length() / 12 * 100;
}

class Client {
    isolated function init(Config config) {
        io:println(config);
    }
}
