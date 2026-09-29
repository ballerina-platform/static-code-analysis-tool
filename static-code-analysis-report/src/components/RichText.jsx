import { Box, Typography } from "@mui/material";
import { Fragment } from "react";
import { tokenizeLines } from "../highlighter";

const LANGUAGE = /^[\w-]*$/;
const INLINE_CODE = /`[^`\n]+`/g;
const HIGHLIGHTED_LANGS = new Set(["", "ballerina", "bal"]);

const inlineCodeSx = {
    fontFamily: "consolas, 'Courier New', monospace",
    fontSize: "0.92em",
    padding: "1px 5px",
    borderRadius: "4px",
    bgcolor: "#eef2f4",
    border: "1px solid var(--surface-border)",
};

const MIN_FENCE_LENGTH = 3;

const countLeadingBackticks = (line) => {
    let count = 0;
    while (count < line.length && line[count] === "`") {
        count++;
    }
    return count;
};

const isClosingFence = (line, fenceLength) => {
    const count = countLeadingBackticks(line);
    return count >= fenceLength && line.slice(count).trim() === "";
};

const findClosingFence = (lines, from, fenceLength) => {
    for (let index = from; index < lines.length; index++) {
        if (isClosingFence(lines[index], fenceLength)) {
            return index;
        }
    }
    return -1;
};

// Splits rule text into prose and ``` fenced code blocks, scanning line by line so it stays linear. A fence
// opens and closes at the start of a line; the closer repeats the opener's backticks (or more) and may only
// be followed by whitespace. Each block keeps the line it starts on, which serves as a stable React key.
const splitBlocks = (text) => {
    const lines = text.split(/\r?\n/);
    const blocks = [];
    let prose = [];
    let proseStart = 0;
    const flushProse = () => {
        if (prose.length > 0) {
            blocks.push({ type: "text", start: proseStart, value: prose.join("\n") });
            prose = [];
        }
    };
    let index = 0;
    while (index < lines.length) {
        const fenceLength = countLeadingBackticks(lines[index]);
        const lang = lines[index].slice(fenceLength).trim();
        const closeAt = fenceLength >= MIN_FENCE_LENGTH && LANGUAGE.test(lang)
            ? findClosingFence(lines, index + 1, fenceLength)
            : -1;
        if (closeAt === -1) {
            if (prose.length === 0) {
                proseStart = index;
            }
            prose.push(lines[index]);
            index += 1;
        } else {
            flushProse();
            blocks.push({ type: "code", start: index, lang: lang.toLowerCase(), value: lines.slice(index + 1, closeAt).join("\n") });
            index = closeAt + 1;
        }
    }
    flushProse();
    return blocks;
};

const InlineText = ({ value }) => {
    const parts = [];
    let last = 0;
    for (const match of value.matchAll(INLINE_CODE)) {
        if (match.index > last) {
            parts.push(<Fragment key={last}>{value.slice(last, match.index)}</Fragment>);
        }
        parts.push(<Box key={match.index} component="code" sx={inlineCodeSx}>{match[0].slice(1, -1)}</Box>);
        last = match.index + match[0].length;
    }
    if (last < value.length) {
        parts.push(<Fragment key={last}>{value.slice(last)}</Fragment>);
    }
    return parts;
};

const CodeBlock = ({ lang, value }) => {
    const lines = value.split("\n");
    const tokenLines = HIGHLIGHTED_LANGS.has(lang)
        ? tokenizeLines(value, lines)
        : lines.map((line) => [{ start: 0, content: line }]);
    // Character offset of each line, used as its key.
    const lineOffsets = [];
    lines.reduce((offset, line) => {
        lineOffsets.push(offset);
        return offset + line.length + 1;
    }, 0);
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
                <div key={lineOffsets[lineIndex]}>
                    {tokens.length === 0 ? " " : tokens.map((token) => (
                        <span key={token.start} style={{ color: token.color }}>{token.content}</span>
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
            {splitBlocks(text).map((block) => block.type === "code"
                ? <CodeBlock key={block.start} lang={block.lang} value={block.value} />
                : block.value.trim() &&
                    <Typography key={block.start} variant={variant} color={color} component="div" sx={{ whiteSpace: "pre-line" }}>
                        <InlineText value={block.value.trim()} />
                    </Typography>
            )}
        </Box>
    );
}

export default RichText;
