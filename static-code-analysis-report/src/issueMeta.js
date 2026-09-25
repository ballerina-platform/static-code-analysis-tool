import {
    BugReportOutlined,
    CodeOffOutlined,
    LockOpenOutlined,
    KeyboardDoubleArrowUp,
    KeyboardArrowUp,
    DragHandle,
    KeyboardArrowDown,
    InfoOutlined,
} from "@mui/icons-material";

// Line/column numbers in the report data are 0-based; everything shown to the user is 1-based.
export const REACT_REPORT_OFFSET = 1;

// Single source of truth for the rule-kind palette used by the cards, tables and code highlights.
export const RULE_KINDS = {
    CODE_SMELL: { label: "Code Smell", plural: "Code Smells", color: "#33B4AF", Icon: CodeOffOutlined },
    BUG: { label: "Bug", plural: "Bugs", color: "#FEAC39", Icon: BugReportOutlined },
    VULNERABILITY: { label: "Vulnerability", plural: "Vulnerabilities", color: "#F74B5A", Icon: LockOpenOutlined },
};

export const RULE_KIND_ORDER = ["CODE_SMELL", "BUG", "VULNERABILITY"];

export const SEVERITIES = {
    BLOCKER: { label: "Blocker", rank: 5, color: "#F74B5A", Icon: KeyboardDoubleArrowUp },
    HIGH: { label: "High", rank: 4, color: "#ff7300", Icon: KeyboardArrowUp },
    MEDIUM: { label: "Medium", rank: 3, color: "#FEAC39", Icon: DragHandle },
    LOW: { label: "Low", rank: 2, color: "#33B4AF", Icon: KeyboardArrowDown },
    INFO: { label: "Info", rank: 1, color: "#757575", Icon: InfoOutlined },
};

// Reports generated before `ruleKind` existed stored the kind under the misnamed `issueSeverity` key.
export const getRuleKind = (issue) => issue?.ruleKind ?? issue?.issueSeverity;

export const getSeverity = (issue) => (SEVERITIES[issue?.severity] ? issue.severity : null);

export const getRuleName = (issue) => issue?.name ?? issue?.message ?? issue?.ruleID;

export const getSeverityRank = (issue) => SEVERITIES[getSeverity(issue)]?.rank ?? 0;

export const cweLabel = (cwe) => `CWE-${cwe}`;

export const cweUrl = (cwe) => `https://cwe.mitre.org/data/definitions/${cwe}.html`;

const OWASP_CATEGORY_NAMES = {
    2017: [
        "Injection", "Broken Authentication", "Sensitive Data Exposure", "XML External Entities (XXE)",
        "Broken Access Control", "Security Misconfiguration", "Cross-Site Scripting (XSS)",
        "Insecure Deserialization", "Using Components with Known Vulnerabilities",
        "Insufficient Logging & Monitoring",
    ],
    2021: [
        "Broken Access Control", "Cryptographic Failures", "Injection", "Insecure Design",
        "Security Misconfiguration", "Vulnerable and Outdated Components",
        "Identification and Authentication Failures", "Software and Data Integrity Failures",
        "Security Logging and Monitoring Failures", "Server-Side Request Forgery (SSRF)",
    ],
    2025: [
        "Broken Access Control", "Security Misconfiguration", "Software Supply Chain Failures",
        "Cryptographic Failures", "Injection", "Insecure Design", "Authentication Failures",
        "Software or Data Integrity Failures", "Security Logging and Alerting Failures",
        "Mishandling of Exceptional Conditions",
    ],
};

// Flattens `[{year, categories: [..]}]` into one entry per category, e.g. {code: "A10:2025", title: "..."}.
export const getOwaspEntries = (issue) => {
    const entries = [];
    (issue?.owasp ?? []).forEach(({ year, categories }) => {
        (categories ?? []).forEach((category) => {
            entries.push({
                code: `A${String(category).padStart(2, "0")}:${year}`,
                title: OWASP_CATEGORY_NAMES[year]?.[category - 1] ?? null,
            });
        });
    });
    return entries;
};

export const getCweList = (issue) => issue?.cwe ?? [];

export const getLineRange = (issue) => {
    const range = issue?.textRange ?? {};
    return {
        startLine: range.startLine + REACT_REPORT_OFFSET,
        endLine: range.endLine + REACT_REPORT_OFFSET,
        startColumn: typeof range.startLineOffset === "number" ? range.startLineOffset : 0,
        endColumn: typeof range.endLineOffset === "number" ? range.endLineOffset : null,
    };
};

export const formatLocation = (issue) => {
    const { startLine, endLine, startColumn, endColumn } = getLineRange(issue);
    const start = `${startLine}:${startColumn + REACT_REPORT_OFFSET}`;
    const end = endColumn === null ? `${endLine}` : `${endLine}:${endColumn + REACT_REPORT_OFFSET}`;
    return startLine === endLine && endColumn !== null
        ? `Ln ${startLine}, Col ${startColumn + REACT_REPORT_OFFSET}–${endColumn + REACT_REPORT_OFFSET}`
        : `${start} → ${end}`;
};

export const countByKind = (issues) => {
    const counts = { CODE_SMELL: 0, BUG: 0, VULNERABILITY: 0 };
    (issues ?? []).forEach((issue) => {
        const kind = getRuleKind(issue);
        if (kind in counts) {
            counts[kind] += 1;
        }
    });
    return counts;
};

// Stable key used in the URL hash to identify a scanned file.
export const getFileKey = (file) => file?.filePath ?? file?.fileName;
