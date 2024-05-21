import {
    BugReportOutlined,
    CodeOffOutlined,
    LockOpenOutlined
} from '@mui/icons-material';
import { IconButton } from '@mui/material';
import Box from '@mui/material/Box';
import {
    DataGrid,
    GridToolbar
} from '@mui/x-data-grid';
import { useEffect, useState } from 'react';

const issueSeverityCell = (cellValue) => {
    switch (cellValue) {
        case "CODE_SMELL":
            return (
                <Box key={cellValue} sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "9rem" }}>
                    <IconButton disabled>
                        <CodeOffOutlined fontSize="large" sx={{ color: "#33B4AF" }} />
                    </IconButton>
                    <strong>Code Smell</strong>
                </Box>
            )
        case "BUG":
            return (
                <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "9rem" }}>
                    <IconButton disabled>
                        <BugReportOutlined fontSize="large" sx={{ color: "#FEAC39" }} />
                    </IconButton>
                    <strong>Bug</strong>
                </Box>
            )
        case "VULNERABILITY":
            return (
                <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "9rem" }}>
                    <IconButton disabled>
                        <LockOpenOutlined fontSize="large" sx={{ color: "#F74B5A" }} />
                    </IconButton>
                    <strong>Vulnerability</strong>
                </Box>
            )
        default:
            return (<Box />)
    }
}

const columns = [
    {
        field: 'ruleID',
        renderHeader: () => {
            return (
                <strong>Rule ID</strong>
            )
        },
        width: 100,
        headerAlign: "left",
        align: "left",
    },
    {
        field: 'issueSeverity',
        renderHeader: () => {
            return (
                <strong>Issue Severity</strong>
            )
        },
        width: 245,
        headerAlign: "right",
        align: "right",
        renderCell: (cell) => issueSeverityCell(cell.value)
    },
    {
        field: 'lineRange',
        renderHeader: () => {
            return (
                <strong>Line Range</strong>
            )
        },
        width: 245,
        headerAlign: "right",
        align: "right",
    },
    {
        field: 'description',
        renderHeader: () => {
            return (
                <strong>Description</strong>
            )
        },
        width: 535,
        headerAlign: "right",
        align: "right",
    }
];

function SingleFileTable({ issues }) {
    const [rows, setRows] = useState([])
    const REACT_REPORT_OFFSET = 1;

    useEffect(() => {
        if (issues?.length !== 0) {
            const issuesArray = [];

            issues?.forEach((issue, issueID) => {
                const newRow = {
                    id: issueID,
                    ruleID: issue.ruleID,
                    issueSeverity: issue.issueSeverity,
                    lineRange: `(${issue.textRange.startLine + REACT_REPORT_OFFSET}:${issue.textRange.startLineOffset}, ${issue.textRange.endLine + REACT_REPORT_OFFSET}:${issue.textRange.endLineOffset})`,
                    description: issue.message
                }

                issuesArray.push(newRow);
            })

            setRows(issuesArray);
        }
    }, [issues])

    return (
        <Box sx={{
            maxWidth: "90%",
            margin: "1rem auto",
        }}>
            <DataGrid
                sx={{
                    padding: "0 2rem",
                    borderRadius: "0.5rem",
                    border: "1px solid var(--primary-color)"
                }}
                rows={rows}
                columns={columns}
                initialState={{
                    pagination: {
                        paginationModel: {
                            pageSize: 5,
                        },
                    },
                }}
                pageSizeOptions={[5, 50, 100]}
                slots={{
                    toolbar: GridToolbar
                }}
                disableColumnMenu={true}
                checkboxSelection={true}
                disableRowSelectionOnClick
                autoHeight
            />
        </Box>
    );
}

export default SingleFileTable;
