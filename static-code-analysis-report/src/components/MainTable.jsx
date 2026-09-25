import { ChevronRight, DescriptionOutlined, FilterAltOutlined } from '@mui/icons-material';
import {
    Typography,
    alpha
} from '@mui/material';
import Box from '@mui/material/Box';
import { useMemo } from 'react';
import {
    DataGrid,
    GridToolbarColumnsButton,
    GridToolbarContainer,
    GridToolbarDensitySelector,
    GridToolbarExport,
    GridToolbarFilterButton,
    GridToolbarQuickFilter
} from '@mui/x-data-grid';
import { ClearFiltersButton, IssueFilterFields, KindFilterChips, SearchField } from './IssueFilters';
import { RULE_KINDS, countActiveFilters } from '../issueMeta';

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

const buildColumns = (filtering) => [
    {
        field: 'fileName',
        headerName: 'File',
        renderHeader: () => <strong>File</strong>,
        valueGetter: (value, row) => row.filePath ?? value,
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
        headerName: filtering ? 'Matching' : 'Total',
        renderHeader: () => <strong>{filtering ? "Matching" : "Total"}</strong>,
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
        hideable: false,
        disableExport: true,
        width: 56,
        align: "center",
        renderCell: () => <ChevronRight sx={{ color: "text.secondary" }} />,
    },
];

const TableToolbar = ({ filtering }) => (
    <GridToolbarContainer sx={{
        padding: "0.5rem 1rem",
        justifyContent: "space-between",
        gap: "0.5rem",
        // Match the toolbar buttons in the single file view's issue table.
        "& .MuiButton-root": { textTransform: "none", fontWeight: 600, borderRadius: "0.5rem" },
    }}>
        <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.25rem" }}>
            <Typography variant="h5" fontWeight="bold" sx={{ marginRight: "0.75rem" }}>
                {filtering ? "Files with matching issues" : "Files with issues"}
            </Typography>
            <GridToolbarColumnsButton />
            <GridToolbarFilterButton />
            <GridToolbarDensitySelector />
            <GridToolbarExport csvOptions={{ fileName: "scan-report-files" }} />
        </Box>
        <GridToolbarQuickFilter debounceMs={200} placeholder="Search files…" />
    </GridToolbarContainer>
)

// Narrows the file list down to files containing issues that match; the same filters are kept when
// a file is opened, so its issue table starts out showing just those issues.
const IssueFilterPanel = ({ allIssues, kindCounts, filters, onFiltersChange }) => (
    <Box sx={{
        display: "flex",
        flexDirection: "column",
        gap: "0.75rem",
        padding: "0.75rem 1rem",
        bgcolor: "var(--page-background)",
        borderBottom: "1px solid var(--surface-border)",
    }}>
        <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", justifyContent: "space-between", gap: "0.75rem" }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                <FilterAltOutlined fontSize="small" color="primary" />
                <Typography variant="h6" fontWeight="bold">Filter by issue</Typography>
            </Box>
            <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.5rem" }}>
                <KindFilterChips
                    counts={kindCounts}
                    selected={filters.kinds}
                    onChange={(kinds) => onFiltersChange({ ...filters, kinds })}
                />
                <SearchField
                    placeholder="Search issues…"
                    value={filters.search}
                    onChange={(search) => onFiltersChange({ ...filters, search })}
                />
            </Box>
        </Box>
        <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.75rem" }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
                <IssueFilterFields
                    issues={allIssues}
                    filters={filters}
                    onChange={onFiltersChange}
                    fields={["tags", "cwes", "owasp", "severities", "rules"]}
                />
            </Box>
            <ClearFiltersButton filters={filters} onChange={onFiltersChange} />
        </Box>
    </Box>
)

function MainTable({ onOpenFile, fileRecords, allIssues, kindCounts, filters, onFiltersChange }) {
    const filtering = countActiveFilters(filters) > 0
    const rows = fileRecords.map((record) => ({ id: record.filePath ?? record.fileName, ...record }))
    const columns = useMemo(() => buildColumns(filtering), [filtering])

    return (
        <Box sx={{
            bgcolor: "#ffffff",
            borderRadius: "0.75rem",
            border: "1px solid var(--primary-color)",
            overflow: "hidden",
        }}>
            <IssueFilterPanel
                allIssues={allIssues}
                kindCounts={kindCounts}
                filters={filters}
                onFiltersChange={onFiltersChange}
            />
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
                    toolbar: TableToolbar,
                    noRowsOverlay: () => (
                        <Box sx={{ display: "flex", height: "100%", alignItems: "center", justifyContent: "center", color: "text.secondary" }}>
                            {filtering ? "No files have issues matching the current filters." : "No files."}
                        </Box>
                    ),
                }}
                slotProps={{ toolbar: { filtering } }}
                disableColumnMenu={true}
                disableRowSelectionOnClick
                autoHeight
                onRowClick={({ row }) => onOpenFile(row.file)}
            />
        </Box>
    );
}

export default MainTable;
