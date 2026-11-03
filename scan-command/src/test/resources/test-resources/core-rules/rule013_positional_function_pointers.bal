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



class Handler {
    final function (string user, string password) callback = connect;

    function run() {
        self.callback("admin", "fieldPointerSecret1");
    }
}

public function main() {
    var inferred = connect;
    inferred("admin", "pointerSecret1");
    function (string user, string password) typed = connect;
    typed("admin", "pointerSecret2");
    function (string, string) unnamed = connect;
    unnamed("admin", "unnamedSecret");
    Handler handler = new;
    handler.callback("admin", "fieldPointerSecret2");
    handler.run();
}

function connect(string user, string password) {
    printValue(user, password);
}

function printValue(any|error... values) {
    _ = values;
}
