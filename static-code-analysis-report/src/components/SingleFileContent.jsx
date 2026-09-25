import { Box } from "@mui/material";
import { useEffect, useState } from "react";

const REACT_REPORT_OFFSET = 1;
const ISSUE_HIGHLIGHT_COLOR = "#F74B5A";

function SingleFileContent({ issues, fileContent }) {
    const [issueRanges, setIssueRanges] = useState([])

    useEffect(() => {
        if (issues?.length !== 0) {
            const ranges = issues?.map((issue) => {
                if (!issue?.textRange
                    || typeof issue.textRange.startLine !== 'number'
                    || typeof issue.textRange.endLine !== 'number') {
                    throw new Error(`Invalid issue format: ${JSON.stringify(issue)}`);
                }
                const startLine = issue.textRange.startLine + REACT_REPORT_OFFSET;
                const endLine = issue.textRange.endLine + REACT_REPORT_OFFSET;
                if (startLine > endLine) {
                    throw new Error(`Invalid line range: startLine (${startLine}) > endLine (${endLine})`);
                }
                return {
                    startLine,
                    endLine,
                    // Falling back to a full-line span keeps older data (without offsets) highlighting as before.
                    startColumn: typeof issue.textRange.startLineOffset === 'number' ? issue.textRange.startLineOffset : 0,
                    endColumn: typeof issue.textRange.endLineOffset === 'number' ? issue.textRange.endLineOffset : null,
                };
            });
            setIssueRanges(ranges);
        } else {
            setIssueRanges([]);
        }
    }, [issues])

    return (
        <ContentMapper fileContent={fileContent} issueRanges={issueRanges} />
    )
}

// Splits a line into plain/highlighted text segments so only the exact issue span within the
// line is colored, instead of the whole line, while still supporting issues spanning several lines.
const getLineSegments = (line, issueRanges, lineNumber) => {
    const relevantRanges = issueRanges.filter(
        ({ startLine, endLine }) => lineNumber >= startLine && lineNumber <= endLine);

    if (line.length === 0) {
        return [{ text: " ", highlighted: relevantRanges.length > 0 }];
    }

    const mask = new Array(line.length).fill(false);
    relevantRanges.forEach(({ startLine, endLine, startColumn, endColumn }) => {
        const from = lineNumber === startLine ? Math.min(startColumn, line.length) : 0;
        const to = lineNumber === endLine ? Math.min(endColumn ?? line.length, line.length) : line.length;
        for (let i = from; i < to; i++) {
            mask[i] = true;
        }
    });

    const segments = [];
    let segmentStart = 0;
    for (let i = 1; i <= line.length; i++) {
        if (i === line.length || mask[i] !== mask[segmentStart]) {
            segments.push({ text: line.slice(segmentStart, i), highlighted: mask[segmentStart] });
            segmentStart = i;
        }
    }
    return segments;
}

const ContentMapper = ({ fileContent, issueRanges }) => {
    // Create a column of line numbers and the content of the file
    return (
        <Box sx={{
            display: "flex",
            flexWrap: "wrap",
            flex: "1",
            width: "80%",
            flexDirection: "column",
            fontFamily: "consolas",
            fontSize: "14px",
            gap: "0.2rem",
            padding: "1rem",
            borderRadius: "0.5rem",
            border: "1px solid var(--primary-color)",
            overflowX: "auto",
        }}>

            {
                fileContent.split("\n").map((line, index) => {
                    const lineNumber = index + REACT_REPORT_OFFSET;
                    const segments = getLineSegments(line, issueRanges, lineNumber);
                    return (
                        <pre
                            key={index}
                            style={{
                                margin: "0",
                                padding: "0.3rem 1rem",
                                backgroundColor: "#f5f5f5",
                            }}>
                            <Box sx={{ display: "flex", gap: "1rem" }}>
                                <Box>
                                    {lineNumber}
                                </Box>
                                <Box>
                                    {segments.map((segment, i) => (
                                        <Box
                                            key={i}
                                            component="span"
                                            sx={segment.highlighted ? { backgroundColor: ISSUE_HIGHLIGHT_COLOR } : undefined}
                                        >
                                            {segment.text}
                                        </Box>
                                    ))}
                                </Box>
                            </Box>
                        </pre>
                    )
                })
            }
        </Box>
    )
}

export default SingleFileContent;
