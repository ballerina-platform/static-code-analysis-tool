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

import io.ballerina.scan.Rule;

import java.util.List;
import java.util.Map;

/**
 * {@code ExternalAnalyzerResult} contains the results of loading the external analyzer plugins.
 *
 * @param externalAnalyzers      the map of loaded external analyzers
 * @param hasAnalyzerPluginIssue indicates whether there was an issue with loading the external analyzers
 *
 * @since 0.1.0
 */
record ExternalAnalyzerResult(Map<String, List<Rule>> externalAnalyzers, boolean hasAnalyzerPluginIssue) { }