import { Box, Fade, Tooltip, Typography, alpha, keyframes } from "@mui/material";
import { CodeOutlined, TouchAppOutlined } from "@mui/icons-material";
import { useEffect, useMemo, useRef, useState } from "react";
import { IssueSummaryCard } from "./IssueBadges";
import { RULE_KINDS, getLineRange, getRuleKind } from "../issueMeta";

// Used when an issue carries an unknown rule kind.
const FALLBACK_HIGHLIGHT_COLOR = "#F74B5A";

const flash = keyframes`
  0%, 100% { background-color: transparent; }
  30% { background-color: rgba(32, 182, 176, 0.28); }
`;

const isValidIssue = (issue) => issue?.textRange
    && typeof issue.textRange.startLine === 'number'
    && typeof issue.textRange.endLine === 'number'
    && issue.textRange.startLine <= issue.textRange.endLine;

function SingleFileContent({ issues, fileContent, selectedIssue, focusRequest, onSelectIssue }) {
    const issueRanges = useMemo(() => (issues ?? []).flatMap((issue, index) => {
        if (!isValidIssue(issue)) {
            console.warn(`Skipping issue with invalid text range: ${JSON.stringify(issue)}`);
            return [];
        }
        const range = getLineRange(issue);
        return [{
            ...range,
            index,
            issue,
            color: RULE_KINDS[getRuleKind(issue)]?.color ?? FALLBACK_HIGHLIGHT_COLOR,
            // Smaller spans win when ranges overlap, so the innermost issue is the one that gets clicked.
            span: (range.endLine - range.startLine) * 100000 + (range.endColumn ?? 10000) - range.startColumn,
        }];
    }), [issues]);

    const lines = useMemo(() => fileContent.split("\n"), [fileContent]);
    const lineRefs = useRef({});
    const [flashLine, setFlashLine] = useState(null);

    useEffect(() => {
        if (focusRequest?.issueIndex === null || focusRequest?.issueIndex === undefined) {
            return;
        }
        const range = issueRanges.find(({ index }) => index === focusRequest.issueIndex);
        const lineElement = range && lineRefs.current[range.startLine];
        if (lineElement) {
            lineElement.scrollIntoView({ behavior: "smooth", block: "center" });
            setFlashLine({ line: range.startLine, nonce: focusRequest.nonce });
        }
    }, [focusRequest, issueRanges]);

    return (
        <Box sx={{
            bgcolor: "#ffffff",
            borderRadius: "0.75rem",
            border: "1px solid var(--primary-color)",
            overflow: "hidden",
        }}>
            <Box sx={{
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "0.5rem",
                padding: "0.75rem 1rem",
                borderBottom: "1px solid var(--surface-border)",
                bgcolor: "var(--page-background)",
            }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <CodeOutlined fontSize="small" color="primary" />
                    <Typography variant="h5" fontWeight="bold">Source</Typography>
                    <Typography variant="body2" color="text.secondary">· {lines.length} lines</Typography>
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: "0.4rem", color: "text.secondary" }}>
                    <TouchAppOutlined sx={{ fontSize: "16px" }} />
                    <Typography variant="body2">Hover a highlight for a summary, click it for full details</Typography>
                </Box>
            </Box>
            <Box sx={{
                maxHeight: "70vh",
                overflow: "auto",
                fontFamily: "consolas, 'Courier New', monospace",
                fontSize: "14px",
                lineHeight: "22px",
                bgcolor: "#fcfdfd",
                padding: "0.5rem 0",
            }}>
                <Box sx={{ minWidth: "max-content" }}>
                    {lines.map((line, index) => {
                        const lineNumber = index + 1;
                        return (
                            <CodeLine
                                key={index}
                                line={line}
                                lineNumber={lineNumber}
                                issueRanges={issueRanges}
                                selectedIssue={selectedIssue}
                                onSelectIssue={onSelectIssue}
                                flashNonce={flashLine?.line === lineNumber ? flashLine.nonce : null}
                                lineRef={(element) => { lineRefs.current[lineNumber] = element; }}
                            />
                        );
                    })}
                </Box>
            </Box>
        </Box>
    )
}

// Splits a line into segments where each segment is covered by the same set of issues, so only the
// exact issue span within the line is colored while still supporting multi-line and overlapping issues.
const getLineSegments = (line, relevantRanges, lineNumber) => {
    if (line.length === 0) {
        return [{ text: " ", ranges: relevantRanges }];
    }

    const coverage = Array.from({ length: line.length }, () => []);
    relevantRanges.forEach((range) => {
        const { startLine, endLine, startColumn, endColumn } = range;
        const from = lineNumber === startLine ? Math.min(startColumn, line.length) : 0;
        const to = lineNumber === endLine ? Math.min(endColumn ?? line.length, line.length) : line.length;
        for (let i = from; i < to; i++) {
            coverage[i].push(range);
        }
    });

    const coverageKey = (ranges) => ranges.map(({ index }) => index).join(",");
    const segments = [];
    let segmentStart = 0;
    for (let i = 1; i <= line.length; i++) {
        if (i === line.length || coverageKey(coverage[i]) !== coverageKey(coverage[segmentStart])) {
            segments.push({ text: line.slice(segmentStart, i), ranges: coverage[segmentStart] });
            segmentStart = i;
        }
    }
    return segments;
}

const bySpan = (a, b) => a.span - b.span;

const CodeLine = ({ line, lineNumber, issueRanges, selectedIssue, onSelectIssue, flashNonce, lineRef }) => {
    const relevantRanges = issueRanges.filter(
        ({ startLine, endLine }) => lineNumber >= startLine && lineNumber <= endLine);
    const segments = getLineSegments(line, relevantRanges, lineNumber);
    const lineMarker = [...relevantRanges].sort(bySpan)[0];
    const selectedRange = relevantRanges.find(({ index }) => index === selectedIssue);

    return (
        <Box
            ref={lineRef}
            sx={{
                display: "flex",
                bgcolor: selectedRange ? alpha(selectedRange.color, 0.07) : "transparent",
                animation: flashNonce !== null ? `${flash} 1.4s ease-in-out` : "none",
                "&:hover": { bgcolor: selectedRange ? alpha(selectedRange.color, 0.1) : "#f3f6f7" },
            }}
            // Re-keying the animation restarts it for repeated "Show in code" requests on the same line.
            key={flashNonce ?? "static"}
        >
            <Box sx={{
                position: "sticky",
                left: 0,
                flexShrink: 0,
                width: "3.75rem",
                paddingRight: "0.9rem",
                textAlign: "right",
                color: lineMarker ? "text.primary" : "text.disabled",
                fontWeight: lineMarker ? 700 : 400,
                bgcolor: "#f3f5f6",
                borderRight: "3px solid",
                borderRightColor: lineMarker ? lineMarker.color : "transparent",
                userSelect: "none",
            }}>
                {lineNumber}
            </Box>
            <Box component="pre" sx={{ margin: 0, padding: "0 1.25rem 0 1rem", whiteSpace: "pre" }}>
                {segments.map((segment, i) => segment.ranges.length === 0
                    ? <span key={i}>{segment.text}</span>
                    : <IssueSegment key={i} segment={segment} selectedIssue={selectedIssue} onSelectIssue={onSelectIssue} />
                )}
            </Box>
        </Box>
    );
};

const IssueSegment = ({ segment, selectedIssue, onSelectIssue }) => {
    const [open, setOpen] = useState(false);
    const ranges = [...segment.ranges].sort(bySpan);
    const selected = ranges.find(({ index }) => index === selectedIssue);
    const primary = selected ?? ranges[0];

    const select = (index) => {
        setOpen(false);
        onSelectIssue(index);
    };

    return (
        <Tooltip
            open={open}
            onOpen={() => setOpen(true)}
            onClose={() => setOpen(false)}
            enterDelay={150}
            enterNextDelay={150}
            placement="top-start"
            // Fade rather than the default Grow: scaling makes the dense card text hard to read mid-transition.
            TransitionComponent={Fade}
            TransitionProps={{ timeout: 120 }}
            disableFocusListener
            describeChild
            slotProps={{
                tooltip: {
                    sx: {
                        bgcolor: "#ffffff",
                        color: "text.primary",
                        padding: "4px",
                        maxWidth: 380,
                        borderRadius: "0.6rem",
                        border: "1px solid var(--surface-border)",
                        boxShadow: "0 8px 24px rgba(0, 0, 0, 0.14)",
                        fontFamily: "inherit",
                    },
                },
            }}
            title={
                <Box sx={{ display: "flex", flexDirection: "column", gap: "2px", maxHeight: "60vh", overflowY: "auto" }}>
                    {ranges.length > 1 &&
                        <Typography variant="caption" color="text.secondary" sx={{ padding: "4px 12px 0" }}>
                            {ranges.length} issues overlap here
                        </Typography>
                    }
                    {ranges.map((range) => (
                        <IssueSummaryCard key={range.index} issue={range.issue} onSelect={() => select(range.index)} />
                    ))}
                </Box>
            }
        >
            <Box
                component="span"
                role="button"
                tabIndex={-1}
                onClick={() => select(primary.index)}
                sx={{
                    cursor: "pointer",
                    borderRadius: "2px",
                    bgcolor: alpha(primary.color, selected ? 0.42 : 0.2 + 0.08 * Math.min(ranges.length - 1, 2)),
                    boxShadow: `inset 0 -2px 0 ${primary.color}`,
                    outline: selected ? `1px solid ${primary.color}` : "none",
                    transition: "background-color 120ms",
                    "&:hover": { bgcolor: alpha(primary.color, 0.45) },
                }}
            >
                {segment.text}
            </Box>
        </Tooltip>
    );
};

export default SingleFileContent;
