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

import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.SingletonTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;

import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_ORG;

/**
 * Helpers on the symbols of the semantic model shared by the core rules.
 *
 * @since 0.13.1
 */
final class SemanticUtils {

    private SemanticUtils() {
    }

    /**
     * Returns the type a type reference, an intersection or a singleton type stands for, such as {@code string}
     * for {@code readonly & Token} where {@code type Token string}.
     */
    static TypeSymbol unwrap(TypeSymbol typeSymbol) {
        return switch (typeSymbol) {
            case TypeReferenceTypeSymbol typeReference -> unwrap(typeReference.typeDescriptor());
            case IntersectionTypeSymbol intersectionType -> unwrap(intersectionType.effectiveTypeDescriptor());
            case SingletonTypeSymbol singletonType -> unwrap(singletonType.originalType());
            default -> typeSymbol;
        };
    }

    static boolean isLangLib(Symbol symbol) {
        return symbol.getModule()
                .map(moduleSymbol -> moduleSymbol.id().orgName().equals(BALLERINA_ORG)
                        && moduleSymbol.id().moduleName().startsWith("lang."))
                .orElse(false);
    }
}
