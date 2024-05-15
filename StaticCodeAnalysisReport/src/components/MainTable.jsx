import {
    BugReportOutlined,
    CodeOffOutlined,
    LockOpenOutlined
} from '@mui/icons-material';
import {
    Typography,
    IconButton
} from '@mui/material';
import Box from '@mui/material/Box';
import {
    DataGrid,
    GridToolbar
} from '@mui/x-data-grid';
import { useEffect, useState } from 'react';

const columns = [
    {
        field: 'fileName',
        renderHeader: () => {
            return (
                <strong>File Name</strong>
            )
        },
        width: 245,
        renderCell: (cellText) => {
            return <Typography variant="p" sx={{
                color: "primary.main",
                fontWeight: "bold",
                cursor: "pointer"
            }}>
                {cellText.value}
            </Typography>
        },
    },
    {
        field: 'codeSmells',
        renderHeader: () => {
            return (
                <Box>
                    <IconButton disabled>
                        <CodeOffOutlined fontSize="large" sx={{ color: "#33B4AF" }} />
                    </IconButton>
                    <strong>Code Smells</strong>
                </Box>
            )
        },
        width: 245,
        headerAlign: "right",
        align: "right",
    },
    {
        field: 'bugs',
        renderHeader: () => {
            return (
                <Box>
                    <IconButton disabled>
                        <BugReportOutlined fontSize="large" sx={{ color: "#FEAC39" }} />
                    </IconButton>
                    <strong>Bugs</strong>
                </Box>
            )
        },
        type: 'number',
        width: 245,
        headerAlign: "right",
        align: "right",
    },
    {
        field: 'vulnerabilities',
        renderHeader: () => {
            return (
                <Box>
                    <IconButton disabled>
                        <LockOpenOutlined fontSize="large" sx={{ color: "#F74B5A" }} />
                    </IconButton>
                    <strong>Vulnerabilities</strong>
                </Box>
            )
        },
        width: 245,
        headerAlign: "right",
        align: "right",
    },
];

function MainTable({ toggleSingleFileView, fileRecords }) {
    const [rows, setRows] = useState([])

    useEffect(() => {
        if (fileRecords?.length !== 0) {
            fileRecords?.forEach((fileRecord, fileID) => {
                const newRow = {
                    id: fileID,
                    fileName: fileRecord.fileName,
                    codeSmells: fileRecord.codeSmells,
                    bugs: fileRecord.bugs,
                    vulnerabilities: fileRecord.vulnerabilities
                }

                setRows(prevRows => [...prevRows, newRow]);
            })
        }
    }, [fileRecords])

    function cellClicked(event) {
        if (event.field === "fileName") {
            toggleSingleFileView(true, event.value)
        }
    }

    return (
        <Box sx={{
            maxWidth: "fit-content",
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
                onCellClick={(e) => { cellClicked(e) }}
            />
        </Box>
    );
}

export default MainTable;
