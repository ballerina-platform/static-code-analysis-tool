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

type Holder record {|
    [@scan:Suppress {rules: ["ballerina:1"], reason: "Tuple member of a field"} int, string] pair;
|};

type Parser function () returns @scan:Suppress {rules: ["ballerina:1"], reason: "Function type"} int;

public function main() {
    @scan:Suppress {rules: ["ballerina:1"], reason: "Destructured variable"}
    [int, int] [x, y] = [checkpanic int:fromString("1"), 2];
    Holder holder = {pair: [x + y, "z"]};
    Parser parser = () => holder.pair[0];
    _ = parser();
}

@scan:Suppress {reason: "Covers the declaration, not the members of the object"}
final object {} task = object {
    function run() returns int {
        int value = checkpanic int:fromString("1");
        return value;
    }
};

@scan:Suppress {reason: "Covers the type, not the members of the object"}
public type Runner readonly & object {
    public function run();
};
