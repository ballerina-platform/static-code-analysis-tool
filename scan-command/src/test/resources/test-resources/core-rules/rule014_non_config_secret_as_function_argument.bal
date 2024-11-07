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

string defaultValue = "password";

public function main() {
    printPasswordStreanght("sabthar", defaultValue);
    printPasswordStreanght("sabthar", password = defaultValue);

    PasswordStrengthChecker checker = new ();
    checker.checkStregth("sabthar", defaultValue); // Can't get expression symbol for defaultValue
    checker.checkStregth("sabthar", password = defaultValue);

    Client 'client = new ("password", defaultValue); // TODO: Find a way to capture this
    'client = new ("sabthar", password = defaultValue);
}

function printPasswordStreanght(string username, string password) {
    io:println(string `Username: ${username}, Password Strength: ${calculateStreanght(password)}%`);
}

function calculateStreanght(string word) returns float {
    return <float>word.length() / 12 * 100;
}

class PasswordStrengthChecker {
    function checkStregth(string username, string password) {
        io:println(string `Username: ${username}, Password Strength: ${calculateStreanght(password)}%`);
    }

    function test() {
        self.checkStregth("sabthar", defaultValue);
        self.checkStregth(username = "sabthar", password = defaultValue);
    }
}

class Client {
    function init(string username, string password) {
        io:println(string `Username: ${username}, Password Strength: ${calculateStreanght(password)}%`);
    }
}
