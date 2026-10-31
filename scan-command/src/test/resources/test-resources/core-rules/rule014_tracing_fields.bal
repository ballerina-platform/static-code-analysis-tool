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



string defaultValue = "default-secret";
configurable Credentials configuredCredentials = ?;
configurable Holder configuredHolder = {value: "holder-default"};
configurable string configuredWithDefault = "configured-default";
configurable string configuredPassword = "configured-password";
configurable string configuredPlaceholder = "changeme";
configurable string configuredFromConstant = BASE_VALUE;

const BASE_VALUE = "base-secret";

type Holder record {|
    string value;
|};

type OptionalHolder record {|
    string value?;
|};

type Middle record {|
    Holder inner;
|};

type Top record {|
    Middle middle;
|};

type Credentials record {|
    string username;
    string password;
|};

public function main() {
    Top top = {middle: {inner: {value: defaultValue}}};
    connect(password = top.middle.inner.value);
    connect(password = top.middle.inner?.value);
    Holder innerHolder = {value: defaultValue};
    Middle viaVariable = {inner: innerHolder};
    connect(password = viaVariable.inner.value);
    OptionalHolder emptyHolder = {};
    connect(password = emptyHolder?.value);
    record {|string value;|} anonymous = {value: defaultValue};
    connect(password = anonymous.value);
    map<string> settings = {value: defaultValue};
    connect(password = settings["value"]);
    map<string> headers = {password: defaultValue};
    Credentials login = {username: "admin", password: defaultValue};
    connect(password = login.password);
    string dbPassword = innerHolder.value;
    connect(password = dbPassword);
    Holder mutated = {value: defaultValue};
    mutated.value = fetch();
    connect(password = mutated.value);
    Credentials creds = {username: "admin", password: fetch()};
    creds.password = defaultValue;
    connect(password = creds.password);
    connect(password = configuredCredentials.password);
    connect(password = configuredHolder.value);
    connect(password = configuredHolder?.value);
    connect(password = configuredWithDefault);
    connect(password = configuredPassword);
    connect(password = configuredPlaceholder);
    connect(password = configuredFromConstant);
    printValue(headers);
}

function connect(string? password) {
    printValue(password);
}

function fetch() returns string => "runtime";

function printValue(any|error... values) {
    _ = values;
}
