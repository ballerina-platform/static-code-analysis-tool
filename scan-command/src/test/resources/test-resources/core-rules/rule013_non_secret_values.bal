// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

import ballerina/http;

// No warning: a boolean as a string does not look generated, as a credential would
function assertCors(http:Response response, string credentials) {
}

// No warning: words, such as "No Proxy", do not look generated
function proxyStatus(http:Request req) returns string {
    string hasAuthHeader = "No Proxy";
    if req.hasHeader("Proxy-Authorization") {
        hasAuthHeader = "Proxy";
    }
    return hasAuthHeader;
}

// No warning: an authentication scheme or header name alone does not look generated
function incompleteHeaders() returns [string, map<string>] {
    string incompleteBasicAuth = "Basic";
    string authHeader = "Authorization";
    return [incompleteBasicAuth + authHeader, {"authorization": "Basic "}];
}

function completeHeaders() returns map<string> {
    return {"authorization": "Basic dXNlcjpwYXNzd29yZA=="}; // warning
}

// A password can be any value, while a key or a token is generated
function readableValues() returns [string, string] {
    string dbPassword = "admin"; // warning
    string apiKey = "admin";
    return [dbPassword, apiKey];
}

// A weak value is not reported where it is written to a token, but it is where it reaches a password
function connect(string password) {
}

function weakValueReachesPassword() {
    string token = "hunter2";
    string dbPassword = token; // warning: ballerina:14
    final map<string> cfg = {token: "hunter2"};
    connect(password = cfg.token); // warning: ballerina:14
    _ = dbPassword;
}

const PART_A = "x7Kq";
const PART_B = "9pZm";

// A value built from fixed parts is checked as a whole
function assembledValues() returns string[] {
    string apiKey = "sk_live_" + "51H8xKq"; // warning
    string accessToken = string `${PART_A}${PART_B}`; // warning
    string clientSecret = "Basic" + " " + "Auth";
    return [apiKey, accessToken, clientSecret];
}

// A key in a map of secrets is checked with the kind of its own name
function mapsOfSecrets() returns map<string>[] {
    map<string> passwords = {apiKey: "true"};
    map<string> tokens = {db: "Welcome1"}; // warning
    return [passwords, tokens];
}

type Scheme string;

const Scheme SCHEME = "Basic";
const "live"|"test" ENVIRONMENT = "live";
const string[] FLAGS = ["true", "false"];

// A constant is checked by its value, whatever its type is written as
function constantValues() returns string[] {
    string apiKey = SCHEME;
    string token = ENVIRONMENT;
    string[] tokens = FLAGS;
    return [apiKey, token, ...tokens];
}

// A key is at least 8 characters, not a run of consecutive characters, and random enough
function generatedLooking() returns string[] {
    string apiKey = "a8f5f167f44f4964"; // warning
    string clientKey = "AKIA1234"; // warning
    string accessKey = "xKqLzPmRtYvWbNcD"; // warning
    string token = "12345678";
    string authToken = "abcdefg1";
    string bearerToken = "Ab3dE5g";
    return [apiKey, clientKey, accessKey, token, authToken, bearerToken];
}

// A value of a map of passwords is reported where it is written, and not again where it is used
function mapOfPasswords() {
    final map<string> passwords = {db: "hunter2"}; // warning
    connect(password = passwords.db);
}

// A prefix or a header name ends with a separator, which does not make it look generated
function separatedNames() returns string[] {
    string authHeader = "Authorization: ";
    string apiKey = "sk_live_";
    string token = "api_key=";
    return [authHeader, apiKey, token];
}

// A concatenation of many parts is checked as a whole
function manyParts() returns string {
    string clientSecret = "a8" + "f5" + "f1" + "67" + "f4" + "4f" + "49" + "64"; // warning
    return clientSecret;
}

public function main() {
    assertCors(new, "true");
}
