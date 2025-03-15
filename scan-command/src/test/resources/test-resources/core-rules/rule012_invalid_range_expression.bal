// Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
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

function testRangeOperator() {
    foreach int i in 0...10 {
        // code
    }

    foreach int i in -5...-2 {
        // code
    }

    foreach int i in 0...-1 { // warning
        // code
    }

    foreach int i in 10...9 { // warning
            // code
    }

    foreach int i in -2...-3 { // warning
            // code
    }

    foreach int i in 0...0 {
        // code
    }

    foreach int i in 10...10 {
        // code
    }

    foreach int i in -2...-2 {
        // code
    }

    foreach int i in 0..<10 {
        // code
    }

    foreach int i in -5..<-2 {
        // code
    }

    foreach int i in 0..<-1 { // warning
        // code
    }

    foreach int i in 10..<9 { // warning
            // code
    }

    foreach int i in -2..<-3 { // warning
            // code
    }

    foreach int i in 0..<0 { // warning
        // code
    }

    foreach int i in 10..<10 { // warning
        // code
    }

    foreach int i in -2..<-2 { // warning
        // code
    }

    _ = from int i in 10..<10 select [1, 2, 3]; // warning

    _ = from int i in -2..<-2 select 1; // warning

    _ = from int i in -2..<2 select 1;

    var val = 10 ... 3; // warning
}
