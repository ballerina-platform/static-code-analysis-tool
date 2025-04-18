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

public function a() { // warning

}

public isolated function a2() {

}

public function main() {

}

function b() {

}

function init() {

}

class A {
    function init() {

    }

    public isolated function c2() {

    }

    public function c() {

    }

    function d() {

    }

    public isolated function main() {

    }
}

public isolated class A2 {
    public isolated function c2() {

    }

    public function init() { // warning

    }

    public function c() { // warning

    }

    function d() {

    }

    public function main() { // warning

    }
}

public isolated class A3 {
    public isolated function c2() {

    }

    public isolated function init() {

    }

    public function c() { // warning

    }

    function d() {

    }

    public function main() { // warning

    }
}

public class A4 { // warning
    function init() {

    }

    public isolated function c2() {

    }

    public function c() { // warning

    }

    function d() {

    }

    public isolated function main() {

    }
}

public isolated service class SC4 {
    function init() {

    }

    public isolated function c2() {

    }

    public function c() { // warning

    }

    function d() {

    }

    public isolated function main() {

    }
}

public service class SC { // warning
    function init() {

    }

    public isolated function c2() {

    }

    public function c() { // warning

    }

    function d() {

    }

    public isolated function main() {

    }
}

public isolated service class SC3 {
    isolated function c2() {

    }
}

isolated service class SC6 {
    isolated function c2() {

    }
}

public service class SC5 { // warning
    isolated function c2() {

    }
}

isolated function a3() {

}
