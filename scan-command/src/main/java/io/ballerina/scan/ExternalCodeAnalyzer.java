/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scan;

import io.ballerina.projects.plugins.CodeAnalyzer;

/**
 * This class provides a convenient abstraction for creating external analyzers
 * that need to provide rules through the service loading mechanism.
 * 
 * <p>
 * External analyzer implementations should extend this class and provide
 * implementations for the abstract methods. The class handles the integration
 * with the Ballerina compiler plugin framework and the static code analysis
 * tool.
 * </p>
 *
 * @since 0.10.0
 */
public abstract class ExternalCodeAnalyzer extends CodeAnalyzer implements RuleProvider {
    /**
     * Default constructor for the external code analyzer.
     * This constructor is used for service loading and should not be called directly.
     */
    protected ExternalCodeAnalyzer() {
        // Default constructor for service loading
    }
}
