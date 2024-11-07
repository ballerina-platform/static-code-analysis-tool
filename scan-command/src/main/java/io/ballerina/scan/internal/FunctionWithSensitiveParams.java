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

import io.ballerina.compiler.api.symbols.Symbol;

import java.util.List;

/**
 * Represents a function that contains sensitive arguments (e.g., secrets, passwords).
 * This record stores the function's symbol and the positions of the arguments
 * that are considered sensitive.
 *
 * <p>The sensitive parameters are tracked by their positions within the function's
 * argument list. This can be used for detecting hardcoded secrets or validating
 * configurations of sensitive data passed to the function.</p>
 *
 * @param symbol               The symbol representing the function.
 * @param sensitiveParamPositions A list of integer positions indicating which
 *                             parameters of the function are considered sensitive.
 */
record FunctionWithSensitiveParams(Symbol symbol, List<Integer> sensitiveParamPositions) {
}
