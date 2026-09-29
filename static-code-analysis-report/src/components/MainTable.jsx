import {
    AccountTreeOutlined,
    ChevronRight,
    DensityMediumOutlined,
    DescriptionOutlined,
    FileDownloadOutlined,
    FilterAltOutlined,
    FilterListOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    Inventory2Outlined,
    KeyboardArrowRight,
    UnfoldLessOutlined,
    UnfoldMoreOutlined,
    ViewColumnOutlined,
    ViewListOutlined
} from '@mui/icons-material';
import {
    Badge,
    Box,
    Button,
    Checkbox,
    Chip,
    Collapse,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography,
    alpha
} from '@mui/material';
import { useMemo, useState } from 'react';
import { ClearFiltersButton, IssueFilterFields, KindFilterChips, SearchField } from './IssueFilters';
import { RULE_KINDS, countActiveFilters } from '../issueMeta';
import {
    buildTree,
    collectFolderIds,
    findPackage,
    flattenList,
    flattenTree,
    getFolderPath,
    getRelativePath,
    packageLabel
} from '../projectTree';

// Filters that live in the collapsible panel; kinds and search sit in the header.
const PANEL_FILTER_KEYS = ["severities", "rules", "tags", "cwes", "owasp"];

const DENSITIES = {
    compact: { label: "Compact", padding: "4px" },
    standard: { label: "Standard", padding: "10px" },
    comfortable: { label: "Comfortable", padding: "16px" },
};

const COUNT_COLUMNS = [
    { field: "codeSmells", kind: "CODE_SMELL" },
    { field: "bugs", kind: "BUG" },
    { field: "vulnerabilities", kind: "VULNERABILITY" },
];

const toolbarButtonSx = { textTransform: "none", fontWeight: 600, borderRadius: "0.5rem", color: "text.secondary" };

const csvEscape = (value) => {
    const text = String(value ?? "");
    return /[",\n\r]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
};

const downloadCsv = (records, packages, workspace) => {
    const header = ["File", ...(workspace ? ["Package"] : []), "Code Smells", "Bugs", "Vulnerabilities", "Total"];
    const rows = records.map((record) => [
        getRelativePath(record.file),
        ...(workspace ? [findPackage(packages, record.file)?.name ?? ""] : []),
        record.codeSmells, record.bugs, record.vulnerabilities, record.totalIssues,
    ]);
    const content = [header, ...rows].map((row) => row.map(csvEscape).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "scan-report-files.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    // Revoking synchronously can cancel the download in some browsers before it starts.
    setTimeout(() => URL.revokeObjectURL(url), 0);
};

// Zero counts fade out so the files that actually need attention stand out.
const KindCount = ({ kind, value }) => (
    <Box sx={{
        display: "inline-block",
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
);

const NodeLabel = ({ node, depth, expanded, listMode }) => {
    const indent = { paddingLeft: `${depth * 1.5}rem` };
    if (node.type === "file") {
        const folder = listMode ? getFolderPath(node.record.file) : "";
        return (
            <Box sx={{ display: "flex", alignItems: "center", gap: "0.6rem", minWidth: 0, ...indent }}>
                {!listMode && <Box sx={{ width: 20, flexShrink: 0 }} />}
                <DescriptionOutlined fontSize="small" color="primary" />
                <Box sx={{ minWidth: 0, lineHeight: 1.25 }}>
                    <Typography variant="body2" noWrap sx={{ color: "primary.main", fontWeight: "bold" }}>
                        {listMode ? node.name.split("/").pop() : node.name}
                    </Typography>
                    {folder &&
                        <Typography variant="caption" color="text.secondary" noWrap component="div">{folder}</Typography>
                    }
                </Box>
            </Box>
        );
    }

    const isPackage = node.type === "package";
    const Icon = isPackage ? Inventory2Outlined : expanded ? FolderOpenOutlined : FolderOutlined;
    return (
        <Box sx={{ display: "flex", alignItems: "center", gap: "0.6rem", minWidth: 0, ...indent }}>
            <KeyboardArrowRight fontSize="small" sx={{
                color: "text.secondary",
                flexShrink: 0,
                transform: expanded ? "rotate(90deg)" : "none",
                transition: "transform 120ms",
            }} />
            <Icon fontSize="small" sx={{ color: isPackage ? "primary.main" : "text.secondary" }} />
            <Box sx={{ minWidth: 0, lineHeight: 1.25 }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <Typography variant="body2" noWrap fontWeight={isPackage ? 700 : 600}>
                        {isPackage ? node.pkg.name : node.name}
                    </Typography>
                    {isPackage && node.pkg.version &&
                        <Chip size="small" label={`v${node.pkg.version}`} sx={{ height: 20, fontSize: "11px", fontWeight: 600 }} />
                    }
                </Box>
                <Typography variant="caption" color="text.secondary" noWrap component="div">
                    {isPackage ? `Package ${packageLabel(node.pkg)} · ` : ""}
                    {node.counts.fileCount} {node.counts.fileCount === 1 ? "file" : "files"}
                </Typography>
            </Box>
        </Box>
    );
};

// Lists files as a folder tree (workspace packages at the top level) or as a flat list. Counts only
// include issues matching the filters, and the same filters carry over when a file is opened.
function MainTable({ onOpenFile, fileRecords, allIssues, kindCounts, filters, onFiltersChange, packages, workspace }) {
    const filtering = countActiveFilters(filters) > 0
    const [viewMode, setViewMode] = useState("tree")
    const [collapsed, setCollapsed] = useState(() => new Set())
    const [sort, setSort] = useState({ field: "totalIssues", direction: "desc" })
    const [page, setPage] = useState(0)
    const [rowsPerPage, setRowsPerPage] = useState(25)
    const [filtersOpen, setFiltersOpen] = useState(() => countActiveFilters(filters, PANEL_FILTER_KEYS) > 0)
    const [hiddenColumns, setHiddenColumns] = useState([])
    const [density, setDensity] = useState("standard")
    const [columnsMenu, setColumnsMenu] = useState(null)
    const [densityMenu, setDensityMenu] = useState(null)

    const listMode = viewMode === "list"
    const tree = useMemo(() => buildTree(fileRecords, packages, workspace), [fileRecords, packages, workspace])
    const folderIds = useMemo(() => collectFolderIds(tree), [tree])
    const allRows = useMemo(
        () => listMode ? flattenList(fileRecords, sort) : flattenTree(tree, collapsed, sort),
        [listMode, fileRecords, tree, collapsed, sort])
    const rows = listMode ? allRows.slice(page * rowsPerPage, (page + 1) * rowsPerPage) : allRows

    const panelFilterCount = countActiveFilters({ ...filters, search: "" }, PANEL_FILTER_KEYS)
    const countColumns = [
        ...COUNT_COLUMNS.map(({ field, kind }) => ({ field, kind, label: RULE_KINDS[kind].plural })),
        { field: "totalIssues", label: filtering ? "Matching" : "Total" },
    ].filter(({ field }) => !hiddenColumns.includes(field))
    const cellPadding = DENSITIES[density].padding

    const toggleFolder = (id) => setCollapsed((prev) => {
        const next = new Set(prev)
        if (next.has(id)) {
            next.delete(id)
        } else {
            next.add(id)
        }
        return next
    })

    const changeSort = (field) => {
        setSort((prev) => prev.field === field
            ? { field, direction: prev.direction === "asc" ? "desc" : "asc" }
            : { field, direction: field === "name" ? "asc" : "desc" })
        setPage(0)
    }

    const toggleColumn = (field) => setHiddenColumns((prev) =>
        prev.includes(field) ? prev.filter((hidden) => hidden !== field) : [...prev, field])

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
                <ToggleButtonGroup
                    size="small"
                    exclusive
                    value={viewMode}
                    onChange={(_, mode) => { if (mode) { setViewMode(mode); setPage(0); } }}
                    aria-label="File view"
                    sx={{ marginRight: "0.5rem", "& .MuiToggleButton-root": { textTransform: "none", fontWeight: 600, padding: "2px 10px", gap: "0.35rem" } }}
                >
                    <ToggleButton value="tree"><AccountTreeOutlined fontSize="small" />Folders</ToggleButton>
                    <ToggleButton value="list"><ViewListOutlined fontSize="small" />List</ToggleButton>
                </ToggleButtonGroup>
                {!listMode && folderIds.length > 0 && <>
                    <Button size="small" startIcon={<UnfoldMoreOutlined />} onClick={() => setCollapsed(new Set())} sx={toolbarButtonSx}>
                        Expand all
                    </Button>
                    <Button size="small" startIcon={<UnfoldLessOutlined />} onClick={() => setCollapsed(new Set(folderIds))} sx={toolbarButtonSx}>
                        Collapse all
                    </Button>
                </>}
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
                        onClick={() => downloadCsv(fileRecords, packages, workspace)}
                        sx={toolbarButtonSx}
                    >
                        Export
                    </Button>
                </Tooltip>
                <Box sx={{ flex: 1 }} />
                <ClearFiltersButton filters={filters} onChange={onFiltersChange} />

                <Menu anchorEl={columnsMenu} open={Boolean(columnsMenu)} onClose={() => setColumnsMenu(null)}>
                    {[...COUNT_COLUMNS.map(({ field, kind }) => ({ field, label: RULE_KINDS[kind].plural })),
                        { field: "totalIssues", label: "Total" }].map(({ field, label }) => (
                        <MenuItem key={field} dense onClick={() => toggleColumn(field)}>
                            <ListItemIcon>
                                <Checkbox size="small" checked={!hiddenColumns.includes(field)} sx={{ padding: 0 }} />
                            </ListItemIcon>
                            <ListItemText>{label}</ListItemText>
                        </MenuItem>
                    ))}
                </Menu>
                <Menu anchorEl={densityMenu} open={Boolean(densityMenu)} onClose={() => setDensityMenu(null)}>
                    {Object.entries(DENSITIES).map(([key, { label }]) => (
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

            <TableContainer>
                <Table size="small" sx={{ minWidth: 760, "& td, & th": { paddingTop: cellPadding, paddingBottom: cellPadding } }}>
                    <TableHead sx={{ bgcolor: "var(--page-background)" }}>
                        <TableRow>
                            <TableCell sortDirection={sort.field === "name" ? sort.direction : false}>
                                <TableSortLabel
                                    active={sort.field === "name"}
                                    direction={sort.field === "name" ? sort.direction : "asc"}
                                    onClick={() => changeSort("name")}
                                    sx={{ fontWeight: 700 }}
                                >
                                    File
                                </TableSortLabel>
                            </TableCell>
                            {countColumns.map(({ field, kind, label }) => {
                                const KindIcon = kind ? RULE_KINDS[kind].Icon : null
                                return (
                                    <TableCell key={field} align="center" sx={{ width: 160 }}
                                        sortDirection={sort.field === field ? sort.direction : false}>
                                        <TableSortLabel
                                            active={sort.field === field}
                                            direction={sort.field === field ? sort.direction : "desc"}
                                            onClick={() => changeSort(field)}
                                            sx={{ fontWeight: 700, gap: "0.4rem", whiteSpace: "nowrap" }}
                                        >
                                            {KindIcon && <KindIcon fontSize="small" sx={{ color: RULE_KINDS[kind].color }} />}
                                            {label}
                                        </TableSortLabel>
                                    </TableCell>
                                )
                            })}
                            <TableCell sx={{ width: 56 }} />
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {rows.length === 0 &&
                            <TableRow>
                                <TableCell colSpan={countColumns.length + 2} align="center" sx={{ padding: "2rem", color: "text.secondary" }}>
                                    {filtering ? "No files have issues matching the current filters." : "No files."}
                                </TableCell>
                            </TableRow>
                        }
                        {rows.map(({ node, depth }) => {
                            const isFile = node.type === "file"
                            const expanded = !isFile && !collapsed.has(node.id)
                            return (
                                <TableRow
                                    key={node.id}
                                    hover
                                    onClick={() => isFile ? onOpenFile(node.record.file) : toggleFolder(node.id)}
                                    aria-expanded={isFile ? undefined : expanded}
                                    sx={{
                                        cursor: "pointer",
                                        bgcolor: node.type === "package" ? alpha("#20b6b0", 0.05) : "transparent",
                                        "&.MuiTableRow-hover:hover": { bgcolor: alpha("#20b6b0", 0.08) },
                                    }}
                                >
                                    <TableCell sx={{ maxWidth: 0, width: "45%" }}>
                                        <NodeLabel node={node} depth={depth} expanded={expanded} listMode={listMode} />
                                    </TableCell>
                                    {countColumns.map(({ field, kind }) => (
                                        <TableCell key={field} align="center">
                                            {kind
                                                ? <KindCount kind={kind} value={node.counts[field]} />
                                                : <strong>{node.counts[field]}</strong>}
                                        </TableCell>
                                    ))}
                                    <TableCell align="center">
                                        {isFile && <ChevronRight sx={{ color: "text.secondary", verticalAlign: "middle" }} />}
                                    </TableCell>
                                </TableRow>
                            )
                        })}
                    </TableBody>
                </Table>
            </TableContainer>
            {listMode &&
                <TablePagination
                    component="div"
                    count={allRows.length}
                    page={Math.min(page, Math.max(0, Math.ceil(allRows.length / rowsPerPage) - 1))}
                    onPageChange={(_, next) => setPage(next)}
                    rowsPerPage={rowsPerPage}
                    onRowsPerPageChange={(event) => { setRowsPerPage(Number(event.target.value)); setPage(0); }}
                    rowsPerPageOptions={[10, 25, 50, 100]}
                />
            }
        </Box>
    );
}

export default MainTable;
