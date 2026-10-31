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

type Holder record {|
    string value;
|};

type Middle record {|
    Holder inner;
|};

configurable Middle configuredMiddle = {inner: {value: "nested-default"}};
configurable Holder configuredHolder = {value: "holder-default"};

public function main() {
    connect(password = configuredMiddle.inner.value);
    connect(password = (configuredHolder).value);
    Holder aliasHolder = configuredHolder;
    connect(password = aliasHolder.value);
    Middle viaConfigurable = {inner: configuredHolder};
    connect(password = viaConfigurable.inner.value);
    Holder inner = {value: defaultValue};
    Middle shorthand = {inner};
    connect(password = shorthand.inner.value);
    Middle chained = {inner: {value: defaultValue}};
    connect(password = chained?.inner.value);
}

function connect(string password) {
    printValue(password);
}

function printValue(any|error... values) {
    _ = values;
}
