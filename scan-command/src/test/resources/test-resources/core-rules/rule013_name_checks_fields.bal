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

type ServiceConfig record {|
    string host = "localhost";
    string password = "rec-default-01";
    string 'secret = "rec-default-02";
    string apiKeyForUrlShortener = "rec-default-03";
    string bearer = string `rec-default-04`;
    string passwordFile = "/run/secrets/service";
    string tokenTypeHint = "access_token";
    string apiKeyHeaderName = "x-api-key";
    string authorName = "Jane Doe";
    string clientSecret = "changeme";
|};

class TokenClient {
    string clientSecret = "obj-field-01";
    string refresh_token = "obj-field-02";
    string authToken = string `obj-field-03`;
    string tokenEndpoint = "https://idp.example.com/token";
    string secretary = "John Doe";
    string tokenEndpointAuthMethod = "client_secret_basic";

    function describe() returns string {
        return self.clientSecret + self.refresh_token + self.authToken + self.tokenEndpoint + self.secretary
            + self.tokenEndpointAuthMethod;
    }
}

public function main() {
    ServiceConfig config = {
        // the password of the reporting user
        host: "db.example.com",
        // client secret
        tokenTypeHint: "refresh_token",
        password: "ctor-field-01",
        passwordFile: "/run/secrets/ctor",
        apiKeyHeaderName: "x-api-key",
        apiKeyForUrlShortener: "ctor-field-02",
        // secret
        authorName: "Jane Doe"
    };
    TokenClient tokenClient = new;
    string description = tokenClient.describe() + config.host;
}
