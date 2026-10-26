import { Box, Chip, Tooltip, Typography, alpha } from "@mui/material";
import {
    RULE_KINDS,
    SEVERITIES,
    SUPPRESSED,
    cweLabel,
    cweUrl,
    getCweList,
    getIssueColor,
    getJustification,
    getOwaspEntries,
    getRuleKind,
    getRuleName,
    getSeverity,
    isSuppressed,
} from "../issueMeta";
import RichText from "./RichText";

const chipBase = {
    height: 24,
    fontWeight: 600,
    fontSize: "12px",
    borderRadius: "6px",
    "& .MuiChip-icon": { fontSize: "16px", marginLeft: "6px" },
};

export const KindChip = ({ issue, kind = getRuleKind(issue) }) => {
    const meta = RULE_KINDS[kind];
    if (!meta) {
        return <Typography variant="body2" color="text.disabled">—</Typography>;
    }
    const { Icon } = meta;
    return (
        <Chip
            size="small"
            icon={<Icon style={{ color: meta.color }} />}
            label={meta.label}
            sx={{ ...chipBase, bgcolor: alpha(meta.color, 0.14), color: "text.primary" }}
        />
    );
};

export const SeverityChip = ({ issue }) => {
    const severity = getSeverity(issue);
    if (!severity) {
        return <Typography variant="body2" color="text.disabled">—</Typography>;
    }
    const meta = SEVERITIES[severity];
    const { Icon } = meta;
    return (
        <Chip
            size="small"
            variant="outlined"
            icon={<Icon style={{ color: meta.color }} />}
            label={meta.label}
            sx={{ ...chipBase, borderColor: alpha(meta.color, 0.6), color: "text.primary", bgcolor: "#ffffff" }}
        />
    );
};

// Marks an issue accepted with `@scan:Suppress`; the tooltip gives the reason when there is one.
export const SuppressedChip = ({ issue }) => {
    if (!isSuppressed(issue)) {
        return null;
    }
    const justification = getJustification(issue);
    return (
        <Tooltip title={justification ? `Suppressed: ${justification}` : "Suppressed without a reason"}>
            <Chip
                size="small"
                icon={<SUPPRESSED.Icon style={{ color: SUPPRESSED.color }} />}
                label={SUPPRESSED.label}
                sx={{ ...chipBase, bgcolor: alpha(SUPPRESSED.color, 0.14), color: "text.primary" }}
            />
        </Tooltip>
    );
};

const standardChipSx = {
    ...chipBase,
    fontWeight: 500,
    fontFamily: "consolas, monospace",
    bgcolor: "#f1f3f4",
    cursor: "pointer",
};

const stopPropagation = (event) => event.stopPropagation();

// `limit` keeps the table cells compact; the expanded row passes no limit to show everything.
export const CweChips = ({ issue, limit, linked = true }) => {
    const cwes = getCweList(issue);
    if (cwes.length === 0) {
        return <Typography variant="body2" color="text.disabled">—</Typography>;
    }
    const shown = limit ? cwes.slice(0, limit) : cwes;
    return (
        <Box sx={{ display: "flex", flexWrap: limit ? "nowrap" : "wrap", gap: "4px" }}>
            {shown.map((cwe) => (
                <Chip
                    key={cwe}
                    size="small"
                    label={cweLabel(cwe)}
                    sx={standardChipSx}
                    {...(linked
                        ? { component: "a", href: cweUrl(cwe), target: "_blank", rel: "noopener noreferrer", clickable: true, onClick: stopPropagation }
                        : {})}
                />
            ))}
            {shown.length < cwes.length && (
                <Tooltip title={cwes.slice(shown.length).map(cweLabel).join(", ")}>
                    <Chip size="small" label={`+${cwes.length - shown.length}`} sx={{ ...chipBase, bgcolor: "#f1f3f4" }} />
                </Tooltip>
            )}
        </Box>
    );
};

export const OwaspChips = ({ issue, limit, showTitles = false, linked = true }) => {
    const entries = getOwaspEntries(issue);
    if (entries.length === 0) {
        return <Typography variant="body2" color="text.disabled">—</Typography>;
    }
    const shown = limit ? entries.slice(0, limit) : entries;
    return (
        <Box sx={{ display: "flex", flexWrap: limit ? "nowrap" : "wrap", gap: "4px" }}>
            {shown.map(({ code, title, url }) => (
                <Tooltip key={code} title={title ?? ""} disableHoverListener={showTitles || !title}>
                    <Chip
                        size="small"
                        label={showTitles && title ? `${code} · ${title}` : code}
                        sx={linked ? standardChipSx : { ...standardChipSx, cursor: "default" }}
                        {...(linked
                            ? { component: "a", href: url, target: "_blank", rel: "noopener noreferrer", clickable: true, onClick: stopPropagation }
                            : {})}
                    />
                </Tooltip>
            ))}
            {shown.length < entries.length && (
                <Tooltip title={entries.slice(shown.length).map((e) => e.code).join(", ")}>
                    <Chip size="small" label={`+${entries.length - shown.length}`} sx={{ ...chipBase, bgcolor: "#f1f3f4" }} />
                </Tooltip>
            )}
        </Box>
    );
};

// Compact card shown when hovering an issue highlight in the code view; clicking the highlight opens the details.
export const IssueSummaryCard = ({ issue }) => {
    const justification = getJustification(issue);
    return (
        <Box sx={{
            padding: "10px 12px",
            borderLeft: `3px solid ${getIssueColor(issue, "var(--primary-color)")}`,
            borderRadius: "4px",
        }}>
            <Typography variant="subtitle2" fontWeight={700} sx={{ lineHeight: 1.3 }}>
                {getRuleName(issue)}
            </Typography>
            {issue.message && issue.message !== getRuleName(issue) &&
                <Box sx={{ marginTop: "4px" }}>
                    <RichText text={issue.message} variant="caption" color="text.secondary" />
                </Box>
            }
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: "6px", marginTop: "8px" }}>
                <KindChip issue={issue} />
                {getSeverity(issue) && <SeverityChip issue={issue} />}
                <SuppressedChip issue={issue} />
            </Box>
            {justification &&
                <Typography variant="caption" color="text.secondary" component="div" sx={{ marginTop: "6px" }}>
                    Reason: {justification}
                </Typography>
            }
        </Box>
    );
};
