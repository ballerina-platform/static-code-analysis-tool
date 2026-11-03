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


import ballerina/crypto;
import ballerina/io;
import ballerina/jwt;

configurable string keyStorePassword = ?;

public function main() returns error? {
    string hashed = check crypto:hashBcrypt("bcryptSecret1");
    boolean matches = check crypto:verifyBcrypt("bcryptSecret2", hashed);
    io:println(hashed, matches);
    crypto:KeyStore keyStore = {path: "keystore.p12", password: keyStorePassword};
    crypto:PrivateKey _ = check crypto:decodeRsaPrivateKeyFromKeyStore(keyStore, "alias", "keyStoreSecret1");
    crypto:PrivateKey _ = check crypto:decodeRsaPrivateKeyFromKeyStore(keyStore, "alias", keyStorePassword);
    jwt:ListenerJwtAuthProvider provider = new ({issuer: "wso2"});
    jwt:Payload _ = check provider.authenticate("jwtCredential1");

    byte[] mac = check crypto:hmacSha256("data".toBytes(), "macKey".toBytes());
    io:println("password", "notASecret", mac);
    check io:fileWriteString("secret.txt", "notASecret");
    crypto:PrivateKey _ = check crypto:decodeRsaPrivateKeyFromKeyFile("key.pem", ());
    string hashedAgain = check crypto:hashBcrypt(hashed, 10);
    io:println(hashedAgain);
}
