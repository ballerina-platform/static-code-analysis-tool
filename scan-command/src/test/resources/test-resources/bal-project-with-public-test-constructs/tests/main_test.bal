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

import ballerina/test;

// No warning: test sources are not part of the public API of the package
public class TestCounter {
    private int count = 0;

    public function increment() {
        self.count += 1;
    }
}

public type Hook object {
    public function run();
};

public function helper() returns int => 1;

@test:Config
function testCounter() {
    TestCounter counter = new;
    counter.increment();
    test:assertEquals(helper(), 1);
}
