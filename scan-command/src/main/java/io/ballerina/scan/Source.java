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

/**
 * Denotes whether an {@link Issue} is reported by the Ballerina platform ({@link #BUILT_IN}) or an
 * external analysis plugin ({@link #EXTERNAL}).
 *
 * @since 0.1.0
 */
public enum Source {
    /**
     * Label for marking issues reported by Ballerina scan tool and Ballerina platform static code analyzer plugins.
     */
    BUILT_IN,

    /**
     * Label for marking issues reported by non Ballerina platform static code analyzer plugins.
     */
    EXTERNAL
}
