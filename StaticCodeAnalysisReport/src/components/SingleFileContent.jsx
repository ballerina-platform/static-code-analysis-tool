import { Box } from "@mui/material";
import { useEffect, useState } from "react";


const REACT_REPORT_OFFSET = 1;
function SingleFileContent({ issues, fileContent }) {
    const [issueLines, setIssueLines] = useState([])

    useEffect(() => {
        if (issues?.length !== 0) {
            issues?.forEach((issue) => {
                const newIssueLine = issue.textRange.startLine + REACT_REPORT_OFFSET;

                setIssueLines(prevIssueLines => [...prevIssueLines, newIssueLine]);
            })
        }
    }, [issues])

    return (
        <ContentMapper fileContent={fileContent} issueLines={issueLines} />
    )
}

const ContentMapper = ({ fileContent, issueLines }) => {
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
                    return (
                        <pre
                            key={index}
                            style={{
                                margin: "0",
                                padding: "0.3rem 1rem",
                                // backgroundColor: "#f5f5f5",
                                backgroundColor: issueLines.includes(index + REACT_REPORT_OFFSET) ? "#F74B5A" : "#f5f5f5",
                            }}>
                            <Box key={index + REACT_REPORT_OFFSET} sx={{ display: "flex", gap: "1rem" }}>
                                <Box>
                                    {index + REACT_REPORT_OFFSET}
                                </Box>
                                <Box>
                                    {line}
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
