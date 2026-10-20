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

import ballerina/scan;

const string KNOWN_RULE = "ballerina:1";
const string UNKNOWN_RULE = "ballerina:999";

public function main() {
    @scan:Suppress {rules: [KNOWN_RULE], reason: "Valid rule through a constant"}
    int a = 1;
    @scan:Suppress {rules: ["ballerina:999"], reason: "Unknown core rule"}
    int b = 2;
    @scan:Suppress {rules: [UNKNOWN_RULE], reason: "Unknown rule through a constant"}
    int c = 3;
    @scan:Suppress {rules: ["http:1"], reason: "Malformed rule ID"}
    int d = 4;
    @scan:Suppress {rules: ["acme/lint:1"], reason: "Provider is not a dependency"}
    int e = 5;
    @scan:Suppress {rules: ["ballerina:1", "ballerina:1"], reason: "Duplicate rule"}
    int f = 6;
    @scan:Suppress {rules: ["ballerina:1"], reason: " "}
    int g = 7;
    @scan:Suppress {rules: [], reason: "Empty rule list"}
    int h = 8;
    _ = a + b + c + d + e + f + g + h;
}
