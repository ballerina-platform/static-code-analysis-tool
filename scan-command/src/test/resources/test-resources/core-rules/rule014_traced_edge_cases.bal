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
string reassignedValue = "first-secret";
string? optionalText = ();

type Holder record {|
    string value;
|};

type OptionalHolder record {|
    string value?;
|};

type Defaulted record {|
    string user;
    string value = "record-default";
|};

type Outer record {|
    Holder inner;
|};

public function main() {
    string step1 = defaultValue;
    string step2 = step1;
    string step3 = step2;
    string step4 = step3;
    string step5 = step4;
    string step6 = step5;
    string step7 = step6;
    connect(password = step7);
    connect(password = ("braced-secret"));
    OptionalHolder holder = {value: defaultValue};
    connect(password = holder?.value);
    Holder quoted = {"value": defaultValue};
    connect(password = quoted.value);
    Holder braced = ({value: defaultValue});
    connect(password = braced.value);
    Outer wrapper = {inner: {value: defaultValue}};
    connect(password = wrapper.inner.value);
    Defaulted defaulted = {user: "admin"};
    connect(password = defaulted.value);
    connect(password = getHolder().value);
    connect(password = optionalText ?: "fallback-secret");
    byte a = 1;
    byte b = 2;
    byte[] secretBytes = [a, b];
    reassignedValue += "-suffix";
    connect(password = reassignedValue);
    [string, string] [first, second] = ["first-secret", "second-secret"];
    connect(password = first);
    foreach string item in [second] {
        connect(password = item);
    }
    printValue(secretBytes);
}

function connect(string? password) {
    printValue(password);
}

function getHolder() returns Holder {
    return {value: "holder-value"};
}

function printValue(any|error... values) {
    _ = values;
}
