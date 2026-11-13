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
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_ORG;

/**
 * Helpers on symbols and names shared by the core rules and the suppression of their findings.
 *
 * @since 0.14.0
 */
final class SemanticUtils {

    private static final Pattern NUMERIC_ESCAPE = Pattern.compile("\\\\u\\{([0-9A-Fa-f]+)}");
    private static final Pattern CHARACTER_ESCAPE = Pattern.compile("\\\\(.)");

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

    // The name a field is written with, as an identifier or a string literal, with escapes decoded
    static String fieldName(SpecificFieldNode field) {
        Node name = field.fieldName();
        String text = name instanceof BasicLiteralNode literal ? literal.literalToken().text() : ((Token) name).text();
        if (text.startsWith("\"")) {
            return unescape(text.substring(1, text.length() - 1));
        }
        return identifierName(text);
    }

    // The name an identifier stands for, without the quote of a quoted identifier and with escapes decoded
    static String identifierName(String text) {
        return unescape(text.startsWith("'") ? text.substring(1) : text);
    }

    // io.ballerina.identifier does not export its unescaping to this module
    private static String unescape(String text) {
        String decoded = NUMERIC_ESCAPE.matcher(text).replaceAll(escape ->
                Matcher.quoteReplacement(Character.toString(Integer.parseInt(escape.group(1), 16))));
        return CHARACTER_ESCAPE.matcher(decoded).replaceAll("$1");
    }

    static boolean isLangLib(Symbol symbol) {
        return symbol.getModule()
                .map(moduleSymbol -> moduleSymbol.id().orgName().equals(BALLERINA_ORG)
                        && moduleSymbol.id().moduleName().startsWith("lang."))
                .orElse(false);
    }
}
