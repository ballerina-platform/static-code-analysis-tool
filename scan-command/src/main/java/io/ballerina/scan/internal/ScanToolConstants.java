/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.internal;

/**
 * {@code ScanToolConstants} contains the constant variables used within the Ballerina scan tool.
 *
 * @since 0.1.0
 */
public class ScanToolConstants {
    static final String SCAN_COMMAND = "scan";
    static final String BALLERINA_RULE_PREFIX = "ballerina:";
    static final String FORWARD_SLASH = "/";
    static final String BALLERINA_ORG = "ballerina";
    static final String BALLERINAI_ORG = "ballerinai";
    static final String BALLERINAX_ORG = "ballerinax";
    static final String USE_IMPORT_AS_UNDERSCORE = " as _;";
    static final String IMPORT_GENERATOR_FILE = "scan_file";
    static final String RULES_FILE = "rules.json";

    private ScanToolConstants() {
    }
}
