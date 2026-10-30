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




public function main(string user, string pass) {
    string[] endpoints = [
        "https://example.com/path",
        "https://admin@example.com",
        "https://admin:@example.com",
        "https://:p4ssw0rd@example.com",
        "https://admin:p4ss@example.com:8443/path?query=1",
        "ftp://admin:ftpP4ss@files.example.com/",
        "redis://default:r3disPass@cache:6379/0",
        "HTTPS://admin:upperP4ss@example.com",
        "https://admin:pa:ss@example.com",
        "https://admin:${DB_PASSWORD}@example.com",
        "https://admin:<password>@example.com",
        "https://admin:changeme@example.com",
        "https://admin:****@example.com",
        "https://admin:your-password@example.com",
        "mailto:admin:p4ss@example.com"
    ];
    string userTemplateUrl = string `https://${user}:s3cr3tP4ss@example.com`;
    string passTemplateUrl = string `https://admin:${pass}@example.com`;
    string bothTemplateUrl = string `https://${user}:${pass}@example.com/${user}`;
    printValue(endpoints, userTemplateUrl, passTemplateUrl, bothTemplateUrl);
}

function printValue(any|error... values) {
    _ = values;
}
