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



string seed = "seed-secret";
string shadowedValue = "module-shadowed-secret";
string rotatedValue = "module-rotated-secret";
final string finalValue = "final-secret";
public final string publicFinalValue = "public-final-secret";
public string publicMutableValue = "public-mutable-secret";
isolated string isolatedValue = "isolated-secret";
isolated string isolatedRotatedValue = "isolated-rotated-secret";
configurable string configuredValue = ?;

public function main() {
    string hop1 = seed;
    string hop2 = hop1;
    string hop3 = hop2;
    string hop4 = hop3;
    connect(password = hop4);
    string hop5 = hop4;
    connect(password = hop5);
    connect(password = laterValue);
    connect(password = finalValue);
    connect(password = publicFinalValue);
    connect(password = publicMutableValue);
    final string localFinal = "local-final-secret";
    connect(password = localFinal);
    string appended = "appended-secret";
    appended += "-suffix";
    connect(password = appended);
    string part = "part-secret";
    connect(password = part + part);
    connect(password = configuredValue + configuredValue);
    connect(password = shadowedValue);
    useIsolated();
}

string laterValue = "declared-later-secret";

function shadowModuleVariable() {
    string shadowedValue = fetch();
    connect(password = shadowedValue);
}

function rotate() {
    rotatedValue = fetch();
    lock {
        isolatedRotatedValue = fetch();
    }
}

function shadowRotatedVariable() {
    string rotatedValue = "local-rotated-secret";
    connect(password = rotatedValue);
}

function reassignLocal() {
    string sameName = "first-local-secret";
    sameName = fetch();
    connect(password = sameName);
}

function keepLocal() {
    string sameName = "second-local-secret";
    connect(password = sameName);
}

function useIsolated() {
    lock {
        connect(password = isolatedValue);
    }
    lock {
        connect(password = isolatedRotatedValue);
    }
}

isolated function connect(string password) {
    printValue(password);
}

isolated function fetch() returns string => "runtime";

isolated function printValue(any|error... values) {
    _ = values;
}
