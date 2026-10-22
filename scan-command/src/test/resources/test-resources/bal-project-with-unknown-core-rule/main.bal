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

public function main() {
    @scan:Suppress {rules: ["ballerina:999"], reason: "Rule the tool does not define"}
    int unknown = 1;
    @scan:Suppress {rules: ["ballerina:1"], reason: "Literal input that always parses"}
    int known = checkpanic int:fromString("1");
    @scan:Suppress {rules: ["acme/lint:1"], reason: "Analyzer the project does not use"}
    int unused = 2;
    _ = unknown + known + unused;
}
