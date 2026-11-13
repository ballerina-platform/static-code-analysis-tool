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

public final string finalValue = "vault-final-secret";
public string mutableValue = "vault-mutable-secret";
public const SHARED_CONSTANT = "vault-constant-secret";
public const string[] API_KEYS = ["a8f5f167f44f4964", "admin"];
public const map<string> DB_PASSWORDS = {primary: "Welcome1", "replica": "Welcome2"};
public const map<string> FIELD_PASSWORDS = {password: "Welcome3"};
public const map<map<string>> NESTED_PASSWORDS = {primary: {db: "Welcome4"}};
public const map<int> PORTS = {primary: 5432};
public const byte[] KEY_BYTES = [97, 56, 102, 53];
public type Creds record {|
    string username;
    string token;
|};

public const Creds DEFAULT_CREDS = {username: "admin", token: "x"};
