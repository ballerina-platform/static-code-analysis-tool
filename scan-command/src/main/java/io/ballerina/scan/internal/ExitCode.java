/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
 * The exit codes of {@code bal scan}, which are the standard exit codes of Ballerina CLI tools. An interrupt exits
 * with {@code 130} through the JVM and needs no code here.
 *
 * @since 0.13.0
 */
enum ExitCode {
    SUCCESS(0),
    RUNTIME_ERROR(1),
    USAGE_ERROR(2),
    INVALID_TARGET(3);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}
