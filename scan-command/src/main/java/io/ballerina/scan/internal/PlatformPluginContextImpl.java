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

import io.ballerina.scan.PlatformPluginContext;

import java.util.Collections;
import java.util.Map;

/**
 * Represents the implementation of the {@link PlatformPluginContext} interface.
 *
 * @param platformArgs in-memory representation of platform specific arguments defined in the Scan.toml file
 * @param initiatedByPlatform true if the plugin is initiated by the platform, false otherwise
 *
 * @since 0.1.0
 */
public record PlatformPluginContextImpl(Map<String, String> platformArgs, boolean initiatedByPlatform) implements
        PlatformPluginContext {
    public PlatformPluginContextImpl {
        platformArgs = Collections.unmodifiableMap(platformArgs);
    }
}
