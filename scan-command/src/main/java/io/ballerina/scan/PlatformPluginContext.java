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

package io.ballerina.scan;

import java.util.Map;

/**
 * {@code PlatformPluginContext} represents a context passed from the scan tool to the platform plugins.
 *
 * @since 0.1.0
 */
public interface PlatformPluginContext {

    /**
     * <p>
     * Returns the platform specific arguments that are defined in addition to the 'name' and 'path' arguments in the
     * Scan.toml file as a map. These arguments can be used for modifying the behavior of the specific platform plugin.
     *</p>
     *
     * @return in-memory representation of the additional platform specific arguments defined in the Scan.toml file.
     */
    Map<String, String> platformArgs();

    /**
     * Returns whether the analysis is initiated by the platform.
     *
     * @return true if the analysis is initiated by the platform, false otherwise.
     */
    boolean initiatedByPlatform();
}
