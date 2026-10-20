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

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Optional;

/**
 * A {@code @scan:Suppress} annotation found in the source, together with the region of the source it covers.
 *
 * @param filePath        path of the document, derived the same way as {@link IssueImpl#filePath()}
 * @param fileName        name of the document as shown in scan results
 * @param location        location of the annotation itself
 * @param scope           location of the annotated construct
 * @param scopeLength     length of the annotated construct, used to find the innermost suppression
 * @param excludedMembers members of an annotated service, class or object type, which the suppression does not cover
 * @param rules           rules named by the annotation; empty when it applies to every rule
 * @param reason          the justification, or {@code null} when absent or blank
 * @since 0.13.0
 */
record SuppressionAnnotation(String filePath, String fileName, LineRange location, LineRange scope, int scopeLength,
                             List<LineRange> excludedMembers, List<String> rules, String reason) {

    SuppressionAnnotation {
        excludedMembers = List.copyOf(excludedMembers);
        rules = List.copyOf(rules);
    }

    boolean appliesTo(String ruleId) {
        return rules.isEmpty() || rules.contains(ruleId);
    }

    boolean covers(LineRange issueRange) {
        if (!contains(scope, issueRange.startLine(), issueRange.endLine())) {
            return false;
        }
        return excludedMembers.stream()
                .noneMatch(member -> contains(member, issueRange.startLine(), issueRange.startLine()));
    }

    Optional<String> justification() {
        return Optional.ofNullable(reason);
    }

    private static boolean contains(LineRange range, LinePosition start, LinePosition end) {
        return compare(range.startLine(), start) <= 0 && compare(end, range.endLine()) <= 0;
    }

    private static int compare(LinePosition a, LinePosition b) {
        return a.line() != b.line() ? Integer.compare(a.line(), b.line()) : Integer.compare(a.offset(), b.offset());
    }
}
