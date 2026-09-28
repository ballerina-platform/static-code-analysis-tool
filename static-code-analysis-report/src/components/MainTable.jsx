import {
    ChevronRight,
    DensityMediumOutlined,
    DescriptionOutlined,
    FileDownloadOutlined,
    FilterAltOutlined,
    FilterListOutlined,
    ViewColumnOutlined
} from '@mui/icons-material';
import {
    Badge,
    Button,
    Checkbox,
    Collapse,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Tooltip,
    Typography,
    alpha
} from '@mui/material';
import Box from '@mui/material/Box';
import { useMemo, useState } from 'react';
import { DataGrid, useGridApiRef } from '@mui/x-data-grid';
import { ClearFiltersButton, IssueFilterFields, KindFilterChips, SearchField } from './IssueFilters';
import { RULE_KINDS, countActiveFilters } from '../issueMeta';

// Filters that live in the collapsible panel; kinds and search sit in the header.
const PANEL_FILTER_KEYS = ["severities", "rules", "tags", "cwes", "owasp"];

const DENSITIES = {
    compact: "Compact",
    standard: "Standard",
    comfortable: "Comfortable",
};

const toolbarButtonSx = { textTransform: "none", fontWeight: 600, borderRadius: "0.5rem", color: "text.secondary" };

const NoRowsOverlay = ({ filtering }) => (
    <Box sx={{ display: "flex", height: "100%", alignItems: "center", justifyContent: "center", color: "text.secondary" }}>
        {filtering ? "No files have issues matching the current filters." : "No files."}
    </Box>
)

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
    headerName: RULE_KINDS[kind].plural,
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

// Narrows the file list down to files containing issues that match; the same filters are kept when
// a file is opened, so its issue table starts out showing just those issues. The toolbar mirrors the
// single file view's issue table, driving the grid through its API instead of the grid's own toolbar.
function MainTable({ onOpenFile, fileRecords, allIssues, kindCounts, filters, onFiltersChange }) {
    const filtering = countActiveFilters(filters) > 0
    const rows = fileRecords.map((record) => ({ id: record.filePath ?? record.fileName, ...record }))
    const columns = useMemo(() => buildColumns(filtering), [filtering])
    const hideableColumns = columns.filter(({ hideable }) => hideable !== false)
    const apiRef = useGridApiRef()
    const [filtersOpen, setFiltersOpen] = useState(() => countActiveFilters(filters, PANEL_FILTER_KEYS) > 0)
    const [columnVisibility, setColumnVisibility] = useState({})
    const [density, setDensity] = useState("standard")
    const [columnsMenu, setColumnsMenu] = useState(null)
    const [densityMenu, setDensityMenu] = useState(null)

    const panelFilterCount = countActiveFilters({ ...filters, search: "" }, PANEL_FILTER_KEYS)
    const visibleColumnCount = hideableColumns.filter(({ field }) => columnVisibility[field] !== false).length

    const toggleColumn = (field) => {
        setColumnVisibility((prev) => ({ ...prev, [field]: prev[field] === false }))
    }

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
                gap: "0.75rem",
                padding: "0.75rem 1rem",
                borderBottom: "1px solid var(--surface-border)",
            }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <FilterAltOutlined fontSize="small" color="primary" />
                    <Typography variant="h5" fontWeight="bold">Filter by issue</Typography>
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

            <Box sx={{
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                gap: "0.25rem",
                padding: "0.25rem 0.75rem",
                borderBottom: "1px solid var(--surface-border)",
            }}>
                <Button
                    size="small"
                    startIcon={<Badge color="primary" variant="dot" invisible={panelFilterCount === 0}><FilterListOutlined /></Badge>}
                    onClick={() => setFiltersOpen((open) => !open)}
                    aria-expanded={filtersOpen}
                    sx={{ ...toolbarButtonSx, color: filtersOpen || panelFilterCount ? "primary.main" : "text.secondary" }}
                >
                    Filters{panelFilterCount > 0 ? ` (${panelFilterCount})` : ""}
                </Button>
                <Button size="small" startIcon={<ViewColumnOutlined />} onClick={(e) => setColumnsMenu(e.currentTarget)} sx={toolbarButtonSx}>
                    Columns
                </Button>
                <Button size="small" startIcon={<DensityMediumOutlined />} onClick={(e) => setDensityMenu(e.currentTarget)} sx={toolbarButtonSx}>
                    Density
                </Button>
                <Tooltip title="Download the files currently listed as CSV">
                    <Button
                        size="small"
                        startIcon={<FileDownloadOutlined />}
                        onClick={() => apiRef.current.exportDataAsCsv({ fileName: "scan-report-files" })}
                        sx={toolbarButtonSx}
                    >
                        Export
                    </Button>
                </Tooltip>
                <Box sx={{ flex: 1 }} />
                <ClearFiltersButton filters={filters} onChange={onFiltersChange} />

                <Menu anchorEl={columnsMenu} open={Boolean(columnsMenu)} onClose={() => setColumnsMenu(null)}>
                    {hideableColumns.map(({ field, headerName }) => {
                        const visible = columnVisibility[field] !== false
                        return (
                            <MenuItem key={field} dense onClick={() => toggleColumn(field)}
                                // Keep at least one column so the grid never collapses to just the open arrow.
                                disabled={visible && visibleColumnCount === 1}>
                                <ListItemIcon>
                                    <Checkbox size="small" checked={visible} sx={{ padding: 0 }} />
                                </ListItemIcon>
                                <ListItemText>{headerName}</ListItemText>
                            </MenuItem>
                        )
                    })}
                </Menu>
                <Menu anchorEl={densityMenu} open={Boolean(densityMenu)} onClose={() => setDensityMenu(null)}>
                    {Object.entries(DENSITIES).map(([key, label]) => (
                        <MenuItem key={key} dense selected={density === key} onClick={() => { setDensity(key); setDensityMenu(null); }}>
                            {label}
                        </MenuItem>
                    ))}
                </Menu>
            </Box>

            <Collapse in={filtersOpen} timeout="auto">
                <Box sx={{ padding: "0.75rem 1rem", bgcolor: "var(--page-background)", borderBottom: "1px solid var(--surface-border)" }}>
                    <IssueFilterFields
                        issues={allIssues}
                        filters={filters}
                        onChange={onFiltersChange}
                        fields={PANEL_FILTER_KEYS}
                    />
                </Box>
            </Collapse>

            <DataGrid
                apiRef={apiRef}
                density={density}
                columnVisibilityModel={columnVisibility}
                onColumnVisibilityModelChange={setColumnVisibility}
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
                slots={{ noRowsOverlay: NoRowsOverlay }}
                slotProps={{ noRowsOverlay: { filtering } }}
                disableColumnMenu={true}
                disableColumnFilter
                disableRowSelectionOnClick
                autoHeight
                onRowClick={({ row }) => onOpenFile(row.file)}
            />
        </Box>
    );
}

export default MainTable;
