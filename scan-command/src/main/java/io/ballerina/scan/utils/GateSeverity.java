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

package io.ballerina.scan.utils;

import io.ballerina.scan.Severity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The severity the quality gate fails on: a rule severity, or {@link #NONE} to turn the gate off.
 *
 * @since 0.13.0
 */
public enum GateSeverity {
    BLOCKER(Severity.BLOCKER),
    HIGH(Severity.HIGH),
    MEDIUM(Severity.MEDIUM),
    LOW(Severity.LOW),
    INFO(Severity.INFO),
    NONE(null);

    /**
     * The values a gate severity can take, as listed in messages.
     */
    public static final String VALUES = "BLOCKER, HIGH, MEDIUM, LOW, INFO or NONE";

    private final Severity severity;

    GateSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Parses a gate severity, ignoring case and surrounding spaces.
     *
     * @param value the severity as given on the command line or in Scan.toml
     * @return the gate severity, or empty if the value is not one
     */
    public static Optional<GateSeverity> parse(String value) {
        String name = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(known -> known.name().equals(name)).findFirst();
    }

    /**
     * Returns the rule severity the gate counts from.
     *
     * @return the rule severity, or empty for {@link #NONE}
     */
    public Optional<Severity> severity() {
        return Optional.ofNullable(severity);
    }
}
