import {
    CodeOutlined,
    DensityMediumOutlined,
    FileDownloadOutlined,
    FilterListOutlined,
    KeyboardArrowDown,
    ListAltOutlined,
    MenuBookOutlined,
    ViewColumnOutlined
} from '@mui/icons-material';
import {
    Badge,
    Box,
    Button,
    Checkbox,
    Chip,
    Collapse,
    IconButton,
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
    Tooltip,
    Typography,
    alpha
} from '@mui/material';
import { Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { CweChips, KindChip, OwaspChips, SeverityChip } from './IssueBadges';
import { ClearFiltersButton, IssueFilterFields, KindFilterChips, SearchField } from './IssueFilters';
import {
    EMPTY_FILTERS,
    RULE_KINDS,
    RULE_KIND_ORDER,
    SEVERITIES,
    countActiveFilters,
    cweLabel,
    formatLocation,
    getCweList,
    getLineRange,
    getOwaspEntries,
    getRuleKind,
    getRuleName,
    getSeverity,
    getSeverityRank,
    matchesFilters
} from '../issueMeta';

const SOURCE_LABELS = {
    BUILT_IN: "Built-in rule",
    EXTERNAL: "External analyzer",
};

// Filters that live in the collapsible panel; kinds and search sit in the header.
const PANEL_FILTER_KEYS = ["severities", "rules", "tags", "cwes", "owasp"];

const DENSITIES = {
    compact: { label: "Compact", padding: "4px" },
    standard: { label: "Standard", padding: "10px" },
    comfortable: { label: "Comfortable", padding: "16px" },
};

const comparators = {
    line: (a, b) => getLineRange(a.issue).startLine - getLineRange(b.issue).startLine
        || getLineRange(a.issue).startColumn - getLineRange(b.issue).startColumn,
    ruleID: (a, b) => String(a.issue.ruleID).localeCompare(String(b.issue.ruleID), undefined, { numeric: true }),
    name: (a, b) => String(getRuleName(a.issue)).localeCompare(String(getRuleName(b.issue))),
    kind: (a, b) => RULE_KIND_ORDER.indexOf(getRuleKind(a.issue)) - RULE_KIND_ORDER.indexOf(getRuleKind(b.issue)),
    severity: (a, b) => getSeverityRank(a.issue) - getSeverityRank(b.issue),
};

const TagChips = ({ issue }) => issue.tags?.length
    ? <Box sx={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
        {issue.tags.map((tag) => <Chip key={tag} size="small" label={tag} sx={{ height: 22, fontSize: "12px" }} />)}
    </Box>
    : <Typography variant="body2" color="text.disabled">—</Typography>;

// `sortField` makes the header sortable; `csv` is the plain-text value used by the export.
const COLUMNS = [
    {
        id: "line", label: "Line", sortField: "line", width: 80,
        cellSx: { fontFamily: "consolas, monospace", fontWeight: 600 },
        render: (issue) => getLineRange(issue).startLine,
        csv: (issue) => formatLocation(issue),
    },
    {
        id: "ruleID", label: "Rule ID", sortField: "ruleID",
        cellSx: { fontFamily: "consolas, monospace", whiteSpace: "nowrap", color: "text.secondary" },
        render: (issue) => issue.ruleID,
        csv: (issue) => issue.ruleID,
    },
    {
        id: "name", label: "Name", sortField: "name",
        cellSx: { fontWeight: 600, minWidth: 200 },
        render: (issue) => getRuleName(issue),
        csv: (issue) => getRuleName(issue),
    },
    {
        id: "kind", label: "Kind", sortField: "kind",
        render: (issue) => <KindChip issue={issue} />,
        csv: (issue) => RULE_KINDS[getRuleKind(issue)]?.label ?? getRuleKind(issue),
    },
    {
        id: "severity", label: "Severity", sortField: "severity",
        render: (issue) => <SeverityChip issue={issue} />,
        csv: (issue) => SEVERITIES[getSeverity(issue)]?.label ?? "",
    },
    {
        id: "cwe", label: "CWE",
        cellSx: { minWidth: 110 },
        render: (issue) => <CweChips issue={issue} limit={2} />,
        csv: (issue) => getCweList(issue).map(cweLabel).join("; "),
    },
    {
        id: "owasp", label: "OWASP",
        cellSx: { minWidth: 110 },
        render: (issue) => <OwaspChips issue={issue} limit={2} />,
        csv: (issue) => getOwaspEntries(issue).map(({ code }) => code).join("; "),
    },
    {
        id: "tags", label: "Tags", hiddenByDefault: true,
        cellSx: { minWidth: 140 },
        render: (issue) => <TagChips issue={issue} />,
        csv: (issue) => (issue.tags ?? []).join("; "),
    },
];

const csvEscape = (value) => {
    const text = String(value ?? "");
    return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
};

const downloadCsv = (issues, fileName) => {
    const header = [...COLUMNS.map(({ label }) => label), "Description"];
    const rows = issues.map(({ issue }) => [...COLUMNS.map(({ csv }) => csv(issue)), issue.details ?? issue.message]);
    const content = [header, ...rows].map((row) => row.map(csvEscape).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `${fileName ?? "issues"}-issues.csv`;
    link.click();
    URL.revokeObjectURL(url);
};

const isInViewport = (element) => {
    const { top, bottom } = element.getBoundingClientRect();
    return top >= 80 && bottom <= window.innerHeight;
};

const toolbarButtonSx = { textTransform: "none", fontWeight: 600, borderRadius: "0.5rem", color: "text.secondary" };

function SingleFileTable({ issues, fileName, selectedIssue, onSelectIssue, onShowInCode, focusRequest, filters, onFiltersChange }) {
    const [sort, setSort] = useState({ field: "line", direction: "asc" });
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [filtersOpen, setFiltersOpen] = useState(() => countActiveFilters(filters, PANEL_FILTER_KEYS) > 0);
    const [hiddenColumns, setHiddenColumns] = useState(
        () => COLUMNS.filter(({ hiddenByDefault }) => hiddenByDefault).map(({ id }) => id));
    const [density, setDensity] = useState("standard");
    const [columnsMenu, setColumnsMenu] = useState(null);
    const [densityMenu, setDensityMenu] = useState(null);
    const rowRefs = useRef({});
    // Issue the table still has to bring into view; set only by explicit reveal requests.
    const pendingReveal = useRef(null);

    const indexedIssues = useMemo(() => (issues ?? []).map((issue, index) => ({ issue, index })), [issues]);

    const visibleIssues = useMemo(() => {
        const comparator = comparators[sort.field];
        const direction = sort.direction === "asc" ? 1 : -1;
        return indexedIssues
            .filter(({ issue }) => matchesFilters(issue, filters))
            // Ties fall back to source order so the list doesn't shuffle between sorts.
            .sort((a, b) => direction * comparator(a, b) || comparators.line(a, b) || a.index - b.index);
    }, [indexedIssues, filters, sort]);

    const visibleColumns = COLUMNS.filter(({ id }) => !hiddenColumns.includes(id));
    const columnCount = visibleColumns.length + 1;
    const cellPadding = DENSITIES[density].padding;

    // Filtering can shrink the list below the current page, so clamp instead of showing an empty page.
    const lastPage = rowsPerPage > 0 ? Math.max(0, Math.ceil(visibleIssues.length / rowsPerPage) - 1) : 0;
    const currentPage = Math.min(page, lastPage);

    useEffect(() => {
        if (focusRequest?.issueIndex !== null && focusRequest?.issueIndex !== undefined) {
            pendingReveal.current = focusRequest.issueIndex;
        }
    }, [focusRequest]);

    // Brings a requested row into view: clear filters that hide it, jump to its page, then scroll to it.
    // This only acts on a pending request, so typing in the search box while an issue is expanded
    // just filters the list instead of snapping the filters back to reveal the selected row.
    useEffect(() => {
        const target = pendingReveal.current;
        if (target === null) {
            return;
        }
        const position = visibleIssues.findIndex(({ index }) => index === target);
        if (position === -1) {
            if (countActiveFilters(filters) === 0) {
                // Not hidden by a filter, so there's nothing to reveal (e.g. a stale index in the URL).
                pendingReveal.current = null;
            } else {
                onFiltersChange(EMPTY_FILTERS);
            }
            return;
        }
        if (rowsPerPage > 0) {
            const targetPage = Math.floor(position / rowsPerPage);
            if (targetPage !== currentPage) {
                setPage(targetPage);
                return;
            }
        }
        pendingReveal.current = null;
        const row = rowRefs.current[target];
        if (row && !isInViewport(row)) {
            row.scrollIntoView({ behavior: "smooth", block: "start" });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [focusRequest, visibleIssues, currentPage, rowsPerPage]);

    const pageIssues = rowsPerPage > 0
        ? visibleIssues.slice(currentPage * rowsPerPage, currentPage * rowsPerPage + rowsPerPage)
        : visibleIssues;

    const kindCounts = useMemo(() => {
        const counts = {};
        indexedIssues.forEach(({ issue }) => {
            const kind = getRuleKind(issue);
            counts[kind] = (counts[kind] ?? 0) + 1;
        });
        return counts;
    }, [indexedIssues]);

    const updateFilters = (next) => {
        setPage(0);
        onFiltersChange(next);
    };

    const changeSort = (field) => {
        setSort((prev) => ({
            field,
            direction: prev.field === field && prev.direction === "asc" ? "desc" : "asc",
        }));
    };

    const toggleColumn = (id) => {
        setHiddenColumns((prev) => prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]);
    };

    const panelFilterCount = countActiveFilters({ ...filters, search: "" }, PANEL_FILTER_KEYS);

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
                    <ListAltOutlined fontSize="small" color="primary" />
                    <Typography variant="h5" fontWeight="bold">Issues</Typography>
                    <Chip size="small" label={visibleIssues.length === indexedIssues.length
                        ? indexedIssues.length
                        : `${visibleIssues.length} of ${indexedIssues.length}`}
                        sx={{ fontWeight: 700, height: 22 }} />
                </Box>
                <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.5rem" }}>
                    <KindFilterChips
                        counts={kindCounts}
                        selected={filters.kinds}
                        onChange={(kinds) => updateFilters({ ...filters, kinds })}
                    />
                    <SearchField
                        placeholder="Search issues…"
                        value={filters.search}
                        onChange={(search) => updateFilters({ ...filters, search })}
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
                <Tooltip title="Download the issues currently listed as CSV">
                    <Button size="small" startIcon={<FileDownloadOutlined />} onClick={() => downloadCsv(visibleIssues, fileName)} sx={toolbarButtonSx}>
                        Export
                    </Button>
                </Tooltip>
                <Box sx={{ flex: 1 }} />
                <ClearFiltersButton filters={filters} onChange={updateFilters} />

                <Menu anchorEl={columnsMenu} open={Boolean(columnsMenu)} onClose={() => setColumnsMenu(null)}>
                    {COLUMNS.map(({ id, label }) => (
                        <MenuItem key={id} dense onClick={() => toggleColumn(id)}
                            // Keep at least one column so the table never collapses to just the expand toggle.
                            disabled={!hiddenColumns.includes(id) && visibleColumns.length === 1}>
                            <ListItemIcon>
                                <Checkbox size="small" checked={!hiddenColumns.includes(id)} sx={{ padding: 0 }} />
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
                        issues={issues}
                        filters={filters}
                        onChange={updateFilters}
                        fields={PANEL_FILTER_KEYS}
                    />
                </Box>
            </Collapse>

            <TableContainer>
                <Table size="small" sx={{ minWidth: 900 }}>
                    <TableHead>
                        <TableRow sx={{
                            bgcolor: "var(--page-background)",
                            "& th": { fontWeight: 700, whiteSpace: "nowrap", paddingTop: "10px", paddingBottom: "10px" },
                        }}>
                            <TableCell sx={{ width: 48 }} />
                            {visibleColumns.map(({ id, label, sortField, width }) => sortField
                                ? <TableCell key={id} sx={{ width }} sortDirection={sort.field === sortField ? sort.direction : false}>
                                    <TableSortLabel
                                        active={sort.field === sortField}
                                        direction={sort.field === sortField ? sort.direction : "asc"}
                                        onClick={() => changeSort(sortField)}
                                    >
                                        {label}
                                    </TableSortLabel>
                                </TableCell>
                                : <TableCell key={id} sx={{ width }}>{label}</TableCell>
                            )}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {pageIssues.length === 0 &&
                            <TableRow>
                                <TableCell colSpan={columnCount} align="center" sx={{ padding: "2rem", color: "text.secondary" }}>
                                    No issues match the current filters.
                                </TableCell>
                            </TableRow>
                        }
                        {pageIssues.map(({ issue, index }) => (
                            <IssueRow
                                key={index}
                                issue={issue}
                                columns={visibleColumns}
                                cellPadding={cellPadding}
                                expanded={index === selectedIssue}
                                onToggle={() => onSelectIssue(index)}
                                onShowInCode={() => onShowInCode(index)}
                                rowRef={(element) => { rowRefs.current[index] = element; }}
                            />
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <TablePagination
                component="div"
                count={visibleIssues.length}
                page={currentPage}
                onPageChange={(_, newPage) => setPage(newPage)}
                rowsPerPage={rowsPerPage}
                onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
                rowsPerPageOptions={[10, 25, 50, { label: "All", value: -1 }]}
                sx={{ borderTop: "1px solid var(--surface-border)" }}
            />
        </Box>
    );
}

const IssueRow = ({ issue, columns, cellPadding, expanded, onToggle, onShowInCode, rowRef }) => {
    const kindColor = RULE_KINDS[getRuleKind(issue)]?.color ?? "#20b6b0";

    return (
        <Fragment>
            <TableRow
                ref={rowRef}
                hover
                onClick={onToggle}
                aria-expanded={expanded}
                sx={{
                    cursor: "pointer",
                    scrollMarginTop: "90px",
                    bgcolor: expanded ? alpha("#20b6b0", 0.07) : "inherit",
                    "& > td": { borderBottom: expanded ? "none" : undefined, paddingTop: cellPadding, paddingBottom: cellPadding },
                    "& > td:first-of-type": { boxShadow: `inset 3px 0 0 ${expanded ? kindColor : "transparent"}` },
                }}
            >
                <TableCell>
                    <IconButton size="small" aria-label={expanded ? "Collapse issue" : "Expand issue"}>
                        <KeyboardArrowDown fontSize="small" sx={{
                            transition: "transform 150ms",
                            transform: expanded ? "rotate(180deg)" : "none",
                        }} />
                    </IconButton>
                </TableCell>
                {columns.map(({ id, cellSx, render }) => (
                    <TableCell key={id} sx={cellSx}>{render(issue)}</TableCell>
                ))}
            </TableRow>
            <TableRow sx={{ bgcolor: alpha("#20b6b0", 0.07) }}>
                <TableCell colSpan={columns.length + 1} sx={{
                    padding: 0,
                    borderBottom: expanded ? undefined : "none",
                    boxShadow: expanded ? `inset 3px 0 0 ${kindColor}` : "none",
                }}>
                    <Collapse in={expanded} timeout="auto" unmountOnExit>
                        <IssueDetails issue={issue} onShowInCode={onShowInCode} />
                    </Collapse>
                </TableCell>
            </TableRow>
        </Fragment>
    );
};

const DetailField = ({ label, children }) => (
    <Box sx={{ display: "flex", flexDirection: "column", gap: "4px" }}>
        <Typography variant="caption" color="text.secondary" fontWeight={700} sx={{ textTransform: "uppercase", letterSpacing: "0.04em" }}>
            {label}
        </Typography>
        <Box sx={{ fontSize: "14px" }}>{children}</Box>
    </Box>
);

const IssueDetails = ({ issue, onShowInCode }) => {
    // The long description already covers the short one, so only fall back to the message when it's missing.
    const description = issue.details ?? issue.message;
    return (
        <Box sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", md: "minmax(0, 3fr) minmax(0, 2fr)" },
            gap: "1.5rem",
            padding: "0.5rem 1.5rem 1.5rem 4.5rem",
        }}>
            <Box sx={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                <DetailField label="Details">
                    <Typography variant="body2" sx={{ whiteSpace: "pre-line", color: description ? "text.primary" : "text.secondary" }}>
                        {description ?? "No further details are provided for this rule."}
                    </Typography>
                </DetailField>
                <Box sx={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                    <Button
                        size="small"
                        variant="contained"
                        disableElevation
                        startIcon={<CodeOutlined />}
                        onClick={(e) => { e.stopPropagation(); onShowInCode(); }}
                        sx={{ color: "#ffffff", textTransform: "none", fontWeight: 600, borderRadius: "0.5rem" }}
                    >
                        Show in code
                    </Button>
                    {issue.helpUri &&
                        <Button
                            size="small"
                            variant="outlined"
                            startIcon={<MenuBookOutlined />}
                            href={issue.helpUri}
                            target="_blank"
                            rel="noopener noreferrer"
                            onClick={(e) => e.stopPropagation()}
                            sx={{ textTransform: "none", fontWeight: 600, borderRadius: "0.5rem", bgcolor: "#ffffff" }}
                        >
                            Rule documentation
                        </Button>
                    }
                </Box>
            </Box>
            <Box sx={{
                display: "grid",
                gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
                gap: "1rem",
                alignContent: "start",
                padding: "1rem",
                bgcolor: "#ffffff",
                borderRadius: "0.6rem",
                border: "1px solid var(--surface-border)",
            }}>
                <DetailField label="Rule ID">
                    <Box component="span" sx={{ fontFamily: "consolas, monospace" }}>{issue.ruleID}</Box>
                </DetailField>
                <DetailField label="Location">
                    <Box component="span" sx={{ fontFamily: "consolas, monospace" }}>{formatLocation(issue)}</Box>
                </DetailField>
                <DetailField label="Kind"><KindChip issue={issue} /></DetailField>
                <DetailField label="Severity"><SeverityChip issue={issue} /></DetailField>
                <DetailField label="Source">{SOURCE_LABELS[issue.issueType] ?? issue.issueType ?? "—"}</DetailField>
                <DetailField label="Tags"><TagChips issue={issue} /></DetailField>
                <Box sx={{ gridColumn: "1 / -1" }}>
                    <DetailField label="CWE"><CweChips issue={issue} /></DetailField>
                </Box>
                <Box sx={{ gridColumn: "1 / -1" }}>
                    <DetailField label="OWASP Top 10"><OwaspChips issue={issue} showTitles /></DetailField>
                </Box>
            </Box>
        </Box>
    );
};

export default SingleFileTable;
