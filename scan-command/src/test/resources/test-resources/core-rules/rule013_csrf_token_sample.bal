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

configurable string baseUrl = ?;
configurable string username = ?;
configurable string password = ?;

type ServiceConfig record {|
    string url;
    string username;
    string password;
|};

type CredentialsConfig record {|
    string username;
    string password;
|};

client class ServiceClient {
    function init(string url, CredentialsConfig auth) {
    }

    remote function get(string path, map<string> headers) returns Response {
        return new (headers);
    }
}

class Response {
    private final map<string> headers;

    function init(map<string> headers) {
        self.headers = headers;
    }

    function getHeader(string name) returns string {
        return self.headers[name] ?: "";
    }
}

public function main() {
    ServiceConfig config = {url: baseUrl, username: username, password: password};
    ServiceClient serviceClient = new (config.url, auth = {username: config.username, password: config.password});
    Response tokenResponse = serviceClient->get(path = "/orders", headers = {"x-csrf-token": "Fetch"});
    string csrfToken = tokenResponse.getHeader("x-csrf-token");
    Response created = serviceClient->get(path = "/orders", headers = {"x-csrf-token": csrfToken});
    _ = created;
}
