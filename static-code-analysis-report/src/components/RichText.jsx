import { Box, Typography } from "@mui/material";
import { Fragment } from "react";
import { tokenizeLines } from "../highlighter";

const FENCE = /```([\w-]*)[^\S\r\n]*\r?\n?([\s\S]*?)```/g;
const INLINE_CODE = /`([^`\n]+)`/g;
const HIGHLIGHTED_LANGS = ["", "ballerina", "bal"];

const inlineCodeSx = {
    fontFamily: "consolas, 'Courier New', monospace",
    fontSize: "0.92em",
    padding: "1px 5px",
    borderRadius: "4px",
    bgcolor: "#eef2f4",
    border: "1px solid var(--surface-border)",
};

// Splits rule text into prose and ``` fenced code blocks.
const splitBlocks = (text) => {
    const blocks = [];
    let last = 0;
    for (const match of text.matchAll(FENCE)) {
        if (match.index > last) {
            blocks.push({ type: "text", value: text.slice(last, match.index) });
        }
        blocks.push({ type: "code", lang: match[1].toLowerCase(), value: match[2].replace(/\r?\n$/, "") });
        last = match.index + match[0].length;
    }
    if (last < text.length) {
        blocks.push({ type: "text", value: text.slice(last) });
    }
    return blocks;
};

const InlineText = ({ value }) => value.split(INLINE_CODE).map((part, index) =>
    // split() with a capture group puts the code spans at odd indexes.
    index % 2 === 1
        ? <Box key={index} component="code" sx={inlineCodeSx}>{part}</Box>
        : <Fragment key={index}>{part}</Fragment>);

const CodeBlock = ({ lang, value }) => {
    const lines = value.split(/\r?\n/);
    const tokenLines = HIGHLIGHTED_LANGS.includes(lang)
        ? tokenizeLines(value, lines)
        : lines.map((line) => [{ start: 0, content: line }]);
    return (
        <Box component="pre" sx={{
            margin: 0,
            padding: "0.6rem 0.8rem",
            overflowX: "auto",
            fontFamily: "consolas, 'Courier New', monospace",
            fontSize: "13px",
            lineHeight: "20px",
            bgcolor: "#fcfdfd",
            border: "1px solid var(--surface-border)",
            borderRadius: "0.5rem",
        }}>
            {tokenLines.map((tokens, lineIndex) => (
                <div key={lineIndex}>
                    {tokens.length === 0 ? " " : tokens.map((token, index) => (
                        <span key={index} style={{ color: token.color }}>{token.content}</span>
                    ))}
                </div>
            ))}
        </Box>
    );
};

// Renders rule descriptions, which use Markdown-style `inline code` and ``` fenced code blocks.
function RichText({ text, variant = "body2", color = "text.primary" }) {
    return (
        <Box sx={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
            {splitBlocks(text).map((block, index) => block.type === "code"
                ? <CodeBlock key={index} lang={block.lang} value={block.value} />
                : block.value.trim() &&
                    <Typography key={index} variant={variant} color={color} component="div" sx={{ whiteSpace: "pre-line" }}>
                        <InlineText value={block.value.trim()} />
                    </Typography>
            )}
        </Box>
    );
}

export default RichText;
