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

// A key in a map of secrets is checked with the stricter of its own kind and the map's, so a map of passwords
// reports any value
function mapsOfSecrets() returns map<string>[] {
    map<string> passwords = {apiKey: "true"}; // warning
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

const byte[] KEY_BYTES = [97, 56, 102, 53];

enum AuthScheme {
    BASIC
}

// A constant of another module is checked by its value
function importedConstants() returns string[] {
    string authToken = http:AUTH_SCHEME_BEARER;
    string dbPassword = http:AUTH_SCHEME_BASIC; // warning
    string apiKey = "a8f5f167f44f4964" + http:AUTH_HEADER; // warning
    return [authToken, dbPassword, apiKey];
}

// A part that is not text, such as an enum member that names a choice, leaves the value unknown
function otherParts() returns string[] {
    string authToken = "x7Kq9pZmA1" + BASIC;
    return [authToken];
}

// A byte array constant is not text, so only a placeholder is skipped
function byteArrayConstant() returns byte[] {
    byte[] secretKey = KEY_BYTES; // warning
    return secretKey;
}

// A spread entry of a map of secrets is not a field, and is checked where its map is written
function spreadEntries(map<string> defaults) returns map<string> {
    map<string> passwords = {...defaults, db: "hunter2"}; // warning
    return passwords;
}

// A password keyword inside a word names a password, whatever word it ends
function embeddedPassword() returns string {
    string dbpasswordvalue = "admin"; // warning
    return dbpasswordvalue;
}

configurable string keyPrefix = "a8f5";
configurable string requiredPart = ?;

// A value built from fixed variables is checked as a whole, and reported as a fixed variable
function partsInVariables() returns string[] {
    final string first = "a8f5";
    final string second = "f167";
    string apiKey = first + second; // warning: ballerina:14
    string accessToken = string `${first}${second}f44f`; // warning: ballerina:14
    string greeting = "No";
    string apiSecret = greeting + " Proxy";
    string token = "a8f5f167f44f4964"; // warning
    string bearerToken = token + "f44f";
    string clientSecret = keyPrefix + "f167"; // warning: a configurable's default is hard-coded
    string authToken = "a8f5" + requiredPart;
    return [apiKey, accessToken, apiSecret, bearerToken, clientSecret, authToken];
}

// A chain of variables longer than the tracing depth is not followed
function longChain() returns string {
    final string p1 = "a8";
    final string p2 = p1 + "f5";
    final string p3 = p2 + "f1";
    final string p4 = p3 + "67";
    final string p5 = p4 + "f4";
    final string p6 = p5 + "4f";
    final string p7 = p6 + "49";
    string apiKey = p7 + "64";
    return apiKey;
}

// Placeholders joined together are still a placeholder, while other parts are joined and checked
function joinedPlaceholders() returns string[] {
    connect(password = "<user>" + "<password>");
    string dbPassword = "changeme" + "changeme";
    string adminPassword = "admin" + "123"; // warning
    string userPassword = "${ENV}-" + "Hunter2!x";
    string token = "sk_live_" + <string>"51H8xKq"; // warning
    string authorization = "Bearer " + "${TOKEN}";
    final record {|string suffix;|} cfg = {suffix: "51H8xKq"};
    string apiKey = "sk_live_" + cfg.suffix; // warning: ballerina:14
    string accessToken = string `sk_live_${cfg["suffix"]}`; // warning: ballerina:14
    return [dbPassword, adminPassword, userPassword, token, authorization, apiKey, accessToken];
}

// A weak secret-named part is not reported where it is written, so it is joined with the other parts
function weakNamedPart() returns string {
    string secretPart = "x8F2";
    string apiKey = "Kq9z" + secretPart; // warning: ballerina:14
    return apiKey;
}

function useToken(string? token) {
}

// A map inside a map of secrets is reported where it is written, and not again where it is used
function nestedMapOfSecrets() {
    map<map<string>> tokens = {github: {dev: "ghp_a1B2c3D4e5F6g7H8"}}; // warning
    useToken(tokens["github"]["dev"]);
}

// Words joined as in a header or a path do not look generated, unlike a base64 value with a slash
function separatedWords() returns string[] {
    string authHeader = "Authorization: Bearer";
    string scheme = "Bearer";
    string authorization = "Authorization: " + scheme;
    string authPath = "oauth/token";
    string apiKey = "Kq9z/x8F2mPq/Lw7R"; // warning
    string clientCredentials = "admin:admin"; // warning
    return [authHeader, authorization, authPath, apiKey, clientCredentials];
}

type Suffix record {|
    string suffix;
|};

type Credentials record {|
    string token;
|};

configurable Suffix configuredSuffix = {suffix: "51H8xKq"};

const Suffix SUFFIX = {suffix: "51H8xKq"};

function suffix() returns Suffix => {suffix: "51H8xKq"};

// A field part is joined with the origin of its mapping, unless it is unknown or was reported where written
function fieldParts() returns string[] {
    string apiKey = "sk_live_" + configuredSuffix.suffix; // warning: a configurable's default is hard-coded
    string accessToken = "sk_live_" + suffix().suffix;
    final Credentials credentials = {token: "51H8xKqLz9Pm"}; // warning
    string bearerToken = "sk_" + credentials.token;
    string clientSecret = "sk_live_" + SUFFIX.suffix; // warning
    return [apiKey, accessToken, bearerToken, clientSecret];
}

// A name that mentions a password names one, whatever keyword it ends with
function passwordNames() returns string[] {
    string dbPasswordSecret = "hunter2"; // warning
    string passwordToken = "admin"; // warning
    return [dbPasswordSecret, passwordToken];
}

// A placeholder made of several parts is a placeholder as a whole
function splitPlaceholders() returns string[] {
    string dbPassword = "change" + "me";
    string adminPassword = "your-" + "password";
    return [dbPassword, adminPassword];
}

function connectWith(string? password) {
}

// A map inside a map of passwords is checked as passwords where it is written, and not again where it is used
function nestedMapOfPasswords() {
    map<map<string>> passwords = {tokens: {github: "hunter2"}}; // warning
    connectWith(passwords["tokens"]["github"]);
}

const VERSION = 20;

// A number or boolean constant is text in a template, not a fixed value of another kind
function scalarConstants() returns string {
    string authHeaderValue = string `v${VERSION}`;
    return authHeaderValue;
}

// A word that contains a keyword but means the opposite does not name a password
function passwordlessNames() returns string {
    string passwordlessAuth = "enabled";
    return passwordlessAuth;
}

const USER = "admin";
const PASS = "secret";
const map<string> AUTH_HEADERS = {"Accept": "application/json"};

function connectAll(string[] passwords) {
}

// A separator part is kept when a value is joined, as the same value written as one literal
function joinedWithSeparator() returns string {
    string authorization = USER + ":" + PASS; // warning
    return authorization;
}

// A long run of letters in one case is not a word, as in a base32 or hexadecimal key
function singleCaseKeys() returns string[] {
    string totpSecret = "JBSWYDPEHPKPXPQR"; // warning
    string hmacSecret = "qwhdkfjsleiruthg"; // warning
    string authToken = "authorization";
    return [totpSecret, hmacSecret, authToken];
}

// A map constant is checked by its values, none of which looks generated
function mapConstant() returns map<string> {
    map<string> authHeaders = AUTH_HEADERS;
    return authHeaders;
}

// Only the members of a list reported where it is written are left out where the list is used
function listMembers() {
    string[] apiKeys = ["admin", "Xk9qLz2PmRtY"]; // warning
    connectAll(passwords = apiKeys); // warning: ballerina:14
}

// A password that qualifies another word, or follows a preposition, does not make the name a password
function qualifiedPasswordNames() returns string[] {
    string tokenForPasswordReset = "reset";
    string passwordPolicyApiKey = "true";
    string passwordLessToken = "magic";
    return [tokenForPasswordReset, passwordPolicyApiKey, passwordLessToken];
}

const map<string> DB_PASSWORDS = {primary: "hunter2"};
const map<string> REPORTED_PASSWORDS = {password: "hunter2"}; // warning
const ID_SUFFIX = "-prod";
type Passwords map<string>;
const Passwords NAMED_PASSWORDS = {primary: "hunter2"};
const map<int> PORTS = {primary: 5432};

function connectMap(map<string> passwords) {
}

function connectPorts(map<int> passwords) {
}

// A map is its values where it is used, less those reported where they are written
function mapValues() {
    connectMap(passwords = DB_PASSWORDS); // warning
    connectMap(passwords = REPORTED_PASSWORDS);
    connectMap(passwords = NAMED_PASSWORDS); // warning
    connectPorts(passwords = PORTS);
    map<string> users = {primary: "admin"};
    connectMap(passwords = users); // warning: ballerina:14
    map<string> dbPasswords = {primary: "hunter2"}; // warning
    connectMap(passwords = dbPasswords);
    map<string> merged = {...users};
    connectMap(passwords = merged);
}

// Only a whole template reference is left out of a joined text, so a "$" in a key does not cut it
function dollarInKey() returns string[] {
    string apiKey = "sk_" + "Zq$RT9XK2LMN"; // warning
    string authToken = "Bearer " + "<token>";
    string bearerToken = "Bearer " + "{{token}}";
    return [apiKey, authToken, bearerToken];
}

// A template reference in one part makes a joined text a placeholder, but a part of one character does not
function templateInPart() returns string[] {
    string password = "user:{{pw}}" + ID_SUFFIX;
    string dbPassword = "p" + "w" + "1"; // warning
    return [password, dbPassword];
}

// A name that starts with a preposition names a password
function prepositionFirst() returns string {
    string inPassword = "admin"; // warning
    return inPassword;
}

public function main() {
    assertCors(new, "true");
}
