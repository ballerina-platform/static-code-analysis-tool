import { createHighlighterCoreSync } from "shiki/core";
import { createJavaScriptRegexEngine } from "shiki/engine/javascript";
import ballerina from "shiki/langs/ballerina.mjs";
import githubLight from "shiki/themes/github-light.mjs";

const THEME = "github-light";

// The JavaScript regex engine avoids loading WASM, which keeps the report usable when opened over file://.
const highlighter = createHighlighterCoreSync({
    themes: [githubLight],
    langs: [ballerina],
    engine: createJavaScriptRegexEngine(),
});

// Returns one token list per line; each token carries its start column within that line.
export function tokenizeLines(code, lines) {
    try {
        const tokenLines = highlighter.codeToTokensBase(code, { lang: "ballerina", theme: THEME });
        return tokenLines.map((tokens) => {
            let column = 0;
            return tokens.map(({ content, color, fontStyle }) => {
                const token = { start: column, content, color, fontStyle };
                column += content.length;
                return token;
            });
        });
    } catch (e) {
        console.warn("Syntax highlighting failed; showing plain source.", e);
        return lines.map((line) => [{ start: 0, content: line }]);
    }
}
