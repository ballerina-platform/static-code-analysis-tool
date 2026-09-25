import { ChevronRight, DescriptionOutlined } from '@mui/icons-material';
import {
    Typography,
    alpha
} from '@mui/material';
import Box from '@mui/material/Box';
import {
    DataGrid,
    GridToolbarContainer,
    GridToolbarQuickFilter
} from '@mui/x-data-grid';
import { RULE_KINDS } from '../issueMeta';

const kindHeader = (kind) => () => {
    const { Icon, plural, color } = RULE_KINDS[kind];
    return (
        <Box sx={{ display: "flex", alignItems: "center", gap: "0.4rem" }}>
            <Icon fontSize="small" sx={{ color }} />
            <strong>{plural}</strong>
        </Box>
    )
}

// Zero counts fade out so the files that actually need attention stand out.
const kindCount = (kind) => ({ value }) => (
    <Box sx={{
        minWidth: "2rem",
        padding: "0.15rem 0.6rem",
        lineHeight: 1.6,
        borderRadius: "999px",
        textAlign: "center",
        fontWeight: 700,
        bgcolor: value > 0 ? alpha(RULE_KINDS[kind].color, 0.16) : "transparent",
        color: value > 0 ? "text.primary" : "text.disabled",
    }}>
        {value}
    </Box>
)

const countColumn = (field, kind) => ({
    field,
    type: 'number',
    renderHeader: kindHeader(kind),
    renderCell: kindCount(kind),
    minWidth: 150,
    flex: 1,
    headerAlign: "center",
    align: "center",
})

const columns = [
    {
        field: 'fileName',
        renderHeader: () => <strong>File</strong>,
        minWidth: 260,
        flex: 2.5,
        renderCell: ({ row }) => {
            return (
                <Box sx={{ display: "flex", alignItems: "center", gap: "0.6rem", height: "100%", minWidth: 0 }}>
                    <DescriptionOutlined fontSize="small" color="primary" />
                    <Box sx={{ minWidth: 0, lineHeight: 1.25 }}>
                        <Typography variant="body2" noWrap sx={{ color: "primary.main", fontWeight: "bold" }}>
                            {row.fileName}
                        </Typography>
                        {row.filePath && row.filePath !== row.fileName &&
                            <Typography variant="caption" color="text.secondary" noWrap component="div" title={row.filePath}>
                                {row.filePath}
                            </Typography>
                        }
                    </Box>
                </Box>
            )
        },
    },
    countColumn('codeSmells', 'CODE_SMELL'),
    countColumn('bugs', 'BUG'),
    countColumn('vulnerabilities', 'VULNERABILITY'),
    {
        field: 'totalIssues',
        type: 'number',
        renderHeader: () => <strong>Total</strong>,
        minWidth: 110,
        flex: 0.8,
        headerAlign: "center",
        align: "center",
        renderCell: ({ value }) => <strong>{value}</strong>,
    },
    {
        field: 'open',
        headerName: '',
        sortable: false,
        filterable: false,
        width: 56,
        align: "center",
        renderCell: () => <ChevronRight sx={{ color: "text.secondary" }} />,
    },
];

const TableToolbar = () => (
    <GridToolbarContainer sx={{ padding: "0.75rem 1rem", justifyContent: "space-between", gap: "0.5rem" }}>
        <Typography variant="h5" fontWeight="bold">Files with issues</Typography>
        <GridToolbarQuickFilter debounceMs={200} placeholder="Search files…" />
    </GridToolbarContainer>
)

function MainTable({ onOpenFile, fileRecords }) {
    const rows = fileRecords.map((record, fileID) => ({ id: fileID, ...record }))

    return (
        <Box sx={{
            bgcolor: "#ffffff",
            borderRadius: "0.75rem",
            border: "1px solid var(--primary-color)",
            overflow: "hidden",
        }}>
            <DataGrid
                sx={{
                    border: "none",
                    "& .MuiDataGrid-columnHeaders": { bgcolor: "var(--page-background)" },
                    "& .MuiDataGrid-row": { cursor: "pointer" },
                    "& .MuiDataGrid-row:hover": { bgcolor: alpha("#20b6b0", 0.06) },
                    "& .MuiDataGrid-cell:focus, & .MuiDataGrid-cell:focus-within": { outline: "none" },
                    "& .MuiDataGrid-cell": { display: "flex", alignItems: "center" },
                    "& .MuiDataGrid-cell[data-field='codeSmells'], & .MuiDataGrid-cell[data-field='bugs'], & .MuiDataGrid-cell[data-field='vulnerabilities'], & .MuiDataGrid-cell[data-field='totalIssues'], & .MuiDataGrid-cell[data-field='open']": { justifyContent: "center" },
                }}
                rows={rows}
                columns={columns}
                rowHeight={60}
                initialState={{
                    pagination: {
                        paginationModel: {
                            pageSize: 10,
                        },
                    },
                    sorting: {
                        sortModel: [{ field: 'totalIssues', sort: 'desc' }],
                    },
                }}
                pageSizeOptions={[10, 25, 50, 100]}
                slots={{
                    toolbar: TableToolbar
                }}
                disableColumnMenu={true}
                disableRowSelectionOnClick
                autoHeight
                onRowClick={({ row }) => onOpenFile(row.file)}
            />
        </Box>
    );
}

export default MainTable;
