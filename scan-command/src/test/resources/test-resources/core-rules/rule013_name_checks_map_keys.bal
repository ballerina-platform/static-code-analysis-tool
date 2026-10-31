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
    map<string> headers = {
        // password for the proxy
        "x-request-id": "map-value-00",
        // api key
        "content-type": "application/json",
        // the token url
        "x-api-key": "map-key-01",
        "X-Auth-Token": "map-key-02",
        "api key": "map-key-03",
        "client.secret": "map-key-04",
        "Proxy-Authorization": "Basic YWxhZGRpbjpvcGVuc2VzYW1l",
        // secret
        "spring.datasource.password": string `map-key-05`,
        apiKey: "map-key-06",
        'passwd: "map-key-07",
        "token url": "https://example.com/oauth/token",
        "db.password.file": "/etc/db/password",
        "x-token-hint": "access_token",
        "oauth.client.id": "client-01",
        "author": "Jane Doe",
        // password
        tokenEndpoint: "https://idp.example.com/token",
        "passcode": "<passcode>"
    };
    map<string> labels = {
        // credentials
        "app": "billing",
        // token
        "tier": "backend"
    };
}
