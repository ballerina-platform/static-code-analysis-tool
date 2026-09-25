import {
    CodeOutlined,
    KeyboardArrowDown,
    MenuBookOutlined,
    Search,
    ListAltOutlined
} from '@mui/icons-material';
import {
    Box,
    Button,
    Chip,
    Collapse,
    IconButton,
    InputAdornment,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    TextField,
    Typography,
    alpha
} from '@mui/material';
import { Fragment, useEffect, useMemo, useRef, useState } from 'react';
import { CweChips, ExternalLink, KindChip, OwaspChips, SeverityChip } from './IssueBadges';
import {
    RULE_KINDS,
    RULE_KIND_ORDER,
    cweLabel,
    formatLocation,
    getCweList,
    getLineRange,
    getOwaspEntries,
    getRuleKind,
    getRuleName,
    getSeverityRank
} from '../issueMeta';

const COLUMN_COUNT = 8;

const SOURCE_LABELS = {
    BUILT_IN: "Built-in rule",
    EXTERNAL: "External analyzer",
};

const comparators = {
    line: (a, b) => getLineRange(a.issue).startLine - getLineRange(b.issue).startLine
        || getLineRange(a.issue).startColumn - getLineRange(b.issue).startColumn,
    ruleID: (a, b) => String(a.issue.ruleID).localeCompare(String(b.issue.ruleID), undefined, { numeric: true }),
    name: (a, b) => String(getRuleName(a.issue)).localeCompare(String(getRuleName(b.issue))),
    kind: (a, b) => RULE_KIND_ORDER.indexOf(getRuleKind(a.issue)) - RULE_KIND_ORDER.indexOf(getRuleKind(b.issue)),
    severity: (a, b) => getSeverityRank(a.issue) - getSeverityRank(b.issue),
};

const matchesSearch = (issue, term) => {
    if (!term) {
        return true;
    }
    const haystack = [
        issue.ruleID,
        getRuleName(issue),
        issue.message,
        issue.details,
        ...(issue.tags ?? []),
        ...getCweList(issue).map(cweLabel),
        ...getOwaspEntries(issue).flatMap(({ code, title }) => [code, title]),
    ].filter(Boolean).join(" ").toLowerCase();
    return haystack.includes(term.toLowerCase());
};

const isInViewport = (element) => {
    const { top, bottom } = element.getBoundingClientRect();
    return top >= 80 && bottom <= window.innerHeight;
};

function SingleFileTable({ issues, selectedIssue, onSelectIssue, onShowInCode }) {
    const [sort, setSort] = useState({ field: "line", direction: "asc" });
    const [kindFilter, setKindFilter] = useState([]);
    const [search, setSearch] = useState("");
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const rowRefs = useRef({});

    const indexedIssues = useMemo(() => (issues ?? []).map((issue, index) => ({ issue, index })), [issues]);

    const visibleIssues = useMemo(() => {
        const comparator = comparators[sort.field];
        const direction = sort.direction === "asc" ? 1 : -1;
        return indexedIssues
            .filter(({ issue }) => kindFilter.length === 0 || kindFilter.includes(getRuleKind(issue)))
            .filter(({ issue }) => matchesSearch(issue, search.trim()))
            // Ties fall back to source order so the list doesn't shuffle between sorts.
            .sort((a, b) => direction * comparator(a, b) || comparators.line(a, b) || a.index - b.index);
    }, [indexedIssues, kindFilter, search, sort]);

    // Filtering can shrink the list below the current page, so clamp instead of showing an empty page.
    const lastPage = rowsPerPage > 0 ? Math.max(0, Math.ceil(visibleIssues.length / rowsPerPage) - 1) : 0;
    const currentPage = Math.min(page, lastPage);

    // When an issue is selected from the code view, make sure its row is actually visible:
    // clear filters that hide it, jump to its page, then scroll it into view.
    useEffect(() => {
        if (selectedIssue === null) {
            return;
        }
        const position = visibleIssues.findIndex(({ index }) => index === selectedIssue);
        if (position === -1) {
            setKindFilter([]);
            setSearch("");
            return;
        }
        if (rowsPerPage > 0) {
            const targetPage = Math.floor(position / rowsPerPage);
            if (targetPage !== currentPage) {
                setPage(targetPage);
                return;
            }
        }
        const row = rowRefs.current[selectedIssue];
        if (row && !isInViewport(row)) {
            row.scrollIntoView({ behavior: "smooth", block: "start" });
        }
        // Only re-run when the selection changes or the row becomes reachable, not on every filter tweak.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedIssue, visibleIssues, currentPage]);

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

    const toggleKind = (kind) => {
        setPage(0);
        setKindFilter((prev) => prev.includes(kind) ? prev.filter((k) => k !== kind) : [...prev, kind]);
    };

    const changeSort = (field) => {
        setSort((prev) => ({
            field,
            direction: prev.field === field && prev.direction === "asc" ? "desc" : "asc",
        }));
    };

    const sortableHeader = (field, label, props = {}) => (
        <TableCell {...props} sortDirection={sort.field === field ? sort.direction : false}>
            <TableSortLabel
                active={sort.field === field}
                direction={sort.field === field ? sort.direction : "asc"}
                onClick={() => changeSort(field)}
            >
                {label}
            </TableSortLabel>
        </TableCell>
    );

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
                    {RULE_KIND_ORDER.filter((kind) => kindCounts[kind]).map((kind) => {
                        const { Icon, color, plural } = RULE_KINDS[kind];
                        const active = kindFilter.includes(kind);
                        return (
                            <Chip
                                key={kind}
                                clickable
                                onClick={() => toggleKind(kind)}
                                icon={<Icon style={{ color: active ? "#ffffff" : color, fontSize: 16 }} />}
                                label={`${plural} · ${kindCounts[kind]}`}
                                variant={active ? "filled" : "outlined"}
                                sx={{
                                    fontWeight: 600,
                                    borderColor: alpha(color, 0.6),
                                    bgcolor: active ? color : "transparent",
                                    color: active ? "#ffffff" : "text.primary",
                                    "&:hover": { bgcolor: active ? color : alpha(color, 0.1) },
                                    "&.MuiChip-clickable:hover": { bgcolor: active ? color : alpha(color, 0.1) },
                                }}
                            />
                        );
                    })}
                    <TextField
                        size="small"
                        placeholder="Search issues…"
                        value={search}
                        onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                        InputProps={{
                            startAdornment: <InputAdornment position="start"><Search fontSize="small" /></InputAdornment>,
                        }}
                        sx={{ width: { xs: "100%", sm: 240 }, "& .MuiOutlinedInput-root": { borderRadius: "0.5rem" } }}
                    />
                </Box>
            </Box>

            <TableContainer>
                <Table size="small" sx={{ minWidth: 900 }}>
                    <TableHead>
                        <TableRow sx={{
                            bgcolor: "var(--page-background)",
                            "& th": { fontWeight: 700, whiteSpace: "nowrap", paddingTop: "10px", paddingBottom: "10px" },
                        }}>
                            <TableCell sx={{ width: 48 }} />
                            {sortableHeader("line", "Line", { sx: { width: 80 } })}
                            {sortableHeader("ruleID", "Rule ID")}
                            {sortableHeader("name", "Name")}
                            {sortableHeader("kind", "Kind")}
                            {sortableHeader("severity", "Severity")}
                            <TableCell>CWE</TableCell>
                            <TableCell>OWASP</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {pageIssues.length === 0 &&
                            <TableRow>
                                <TableCell colSpan={COLUMN_COUNT} align="center" sx={{ padding: "2rem", color: "text.secondary" }}>
                                    No issues match the current filters.
                                </TableCell>
                            </TableRow>
                        }
                        {pageIssues.map(({ issue, index }) => (
                            <IssueRow
                                key={index}
                                issue={issue}
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

const IssueRow = ({ issue, expanded, onToggle, onShowInCode, rowRef }) => {
    const kindColor = RULE_KINDS[getRuleKind(issue)]?.color ?? "#20b6b0";
    const { startLine } = getLineRange(issue);

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
                    "& > td": { borderBottom: expanded ? "none" : undefined, paddingTop: "10px", paddingBottom: "10px" },
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
                <TableCell sx={{ fontFamily: "consolas, monospace", fontWeight: 600 }}>{startLine}</TableCell>
                <TableCell sx={{ fontFamily: "consolas, monospace", whiteSpace: "nowrap", color: "text.secondary" }}>
                    {issue.ruleID}
                </TableCell>
                <TableCell sx={{ fontWeight: 600, minWidth: 200 }}>{getRuleName(issue)}</TableCell>
                <TableCell><KindChip issue={issue} /></TableCell>
                <TableCell><SeverityChip issue={issue} /></TableCell>
                <TableCell sx={{ minWidth: 110 }}><CweChips issue={issue} limit={2} /></TableCell>
                <TableCell sx={{ minWidth: 110 }}><OwaspChips issue={issue} limit={2} /></TableCell>
            </TableRow>
            <TableRow sx={{ bgcolor: alpha("#20b6b0", 0.07) }}>
                <TableCell colSpan={COLUMN_COUNT} sx={{
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

const IssueDetails = ({ issue, onShowInCode }) => (
    <Box sx={{
        display: "grid",
        gridTemplateColumns: { xs: "1fr", md: "minmax(0, 3fr) minmax(0, 2fr)" },
        gap: "1.5rem",
        padding: "0.5rem 1.5rem 1.5rem 4.5rem",
    }}>
        <Box sx={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
            <DetailField label="Short description">
                <Typography variant="body2">{issue.message ?? "—"}</Typography>
            </DetailField>
            <DetailField label="Long description">
                <Typography variant="body2" sx={{ whiteSpace: "pre-line", color: issue.details ? "text.primary" : "text.secondary" }}>
                    {issue.details ?? "No further details are provided for this rule."}
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
            <DetailField label="Tags">
                {issue.tags?.length
                    ? <Box sx={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
                        {issue.tags.map((tag) => <Chip key={tag} size="small" label={tag} sx={{ height: 22, fontSize: "12px" }} />)}
                    </Box>
                    : <Typography variant="body2" color="text.disabled">—</Typography>}
            </DetailField>
            <Box sx={{ gridColumn: "1 / -1" }}>
                <DetailField label="CWE"><CweChips issue={issue} /></DetailField>
            </Box>
            <Box sx={{ gridColumn: "1 / -1" }}>
                <DetailField label="OWASP Top 10"><OwaspChips issue={issue} showTitles /></DetailField>
            </Box>
            {issue.helpUri &&
                <Box sx={{ gridColumn: "1 / -1" }}>
                    <DetailField label="Help">
                        <ExternalLink href={issue.helpUri}>{issue.helpUri}</ExternalLink>
                    </DetailField>
                </Box>
            }
        </Box>
    </Box>
);

export default SingleFileTable;
