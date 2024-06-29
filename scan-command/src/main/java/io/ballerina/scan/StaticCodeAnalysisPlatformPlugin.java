/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

import java.util.List;

/**
 * {@code StaticCodeAnalysisPlatformPlugin} Represents the interface to extend to report issues for a specific platform.
 *
 * @since 0.1.0
 */
public interface StaticCodeAnalysisPlatformPlugin {

    /**
     * Returns the platform name.
     *
     * @return platform name
     */
    String platform();

    /**
     * Initializes the platform plugin with a {@link PlatformPluginContext}.
     *
     * @param platformPluginContext context passed from the scan tool to the platform plugins.
     */
    void init(PlatformPluginContext platformPluginContext);

    /**
     * The method that gets invoked when the scan is complete.
     *
     * @param issues list of issues passed from the scan tool.
     */
    void onScan(List<Issue> issues);
}
