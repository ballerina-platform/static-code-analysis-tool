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




public function main() {
    string text = "password";
    printValue(text.substring(1, 3), text.concat("token", "secret"), text.padStart(12, "*"));
    printValue(string:concat("password", "secret"), text.includes("secret", 0), text.startsWith("token"));
    map<string> entries = {user: text};
    printValue(entries.hasKey("password"), entries.get("password"), entries["token"]);
    printValue(entries.removeIfHasKey("password"));
    string[] values = [];
    values.push("first", "second");
    printValue(values.indexOf("first"), values.slice(0, 1));
    error failure = error("secret", message = "token");
    printValue(failure.message(), failure.detail().toString());
}

function printValue(any|error... values) {
    _ = values;
}
