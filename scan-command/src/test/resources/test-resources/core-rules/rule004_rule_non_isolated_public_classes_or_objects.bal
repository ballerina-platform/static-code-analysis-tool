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

class A {

}

public class B { // warning

}

isolated class C {

}

public isolated class D {

}

type E object {
    public function hash() returns int;
    function hash2() returns int;
};

public type F object { // warning
    public function hash() returns int;
    function hash2() returns int;
};

isolated type G object {
    public isolated function hash() returns int;
    function hash2() returns int;
};

public type Hashable isolated object {
    public function hash() returns int;
    function hash2() returns int;
};

type H object {
    public isolated function hash() returns int;
    function hash2() returns int;
};

public type I object { // warning
    public isolated function hash() returns int;
    function hash2() returns int;
};

isolated service class A {

}

service class SA2 {

}

public isolated service class SA3 {

}

public service class SA4 { // warning

}
