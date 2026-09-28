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

// Page slugs on owasp.org/Top10, which don't always match the display names above (e.g. the SSRF parentheses).
const OWASP_CATEGORY_SLUGS = {
    2021: [
        "Broken_Access_Control", "Cryptographic_Failures", "Injection", "Insecure_Design",
        "Security_Misconfiguration", "Vulnerable_and_Outdated_Components",
        "Identification_and_Authentication_Failures", "Software_and_Data_Integrity_Failures",
        "Security_Logging_and_Monitoring_Failures", "Server-Side_Request_Forgery_%28SSRF%29",
    ],
    2025: [
        "Broken_Access_Control", "Security_Misconfiguration", "Software_Supply_Chain_Failures",
        "Cryptographic_Failures", "Injection", "Insecure_Design", "Authentication_Failures",
        "Software_or_Data_Integrity_Failures", "Security_Logging_and_Alerting_Failures",
        "Mishandling_of_Exceptional_Conditions",
    ],
};

const owaspUrl = (year, category) => {
    const code = `A${String(category).padStart(2, "0")}_${year}`;
    const slug = OWASP_CATEGORY_SLUGS[year]?.[category - 1];
    if (!slug) {
        return "https://owasp.org/www-project-top-ten/";
    }
    // Newer editions live under a year folder; 2021 is still served from the root.
    return String(year) === "2021"
        ? `https://owasp.org/Top10/${code}-${slug}/`
        : `https://owasp.org/Top10/${year}/${code}-${slug}/`;
};

// Flattens `[{year, categories: [..]}]` into one entry per category, e.g. {code: "A10:2025", title: "...", url: "..."}.
export const getOwaspEntries = (issue) => {
    const entries = [];
    (issue?.owasp ?? []).forEach(({ year, categories }) => {
        (categories ?? []).forEach((category) => {
            entries.push({
                code: `A${String(category).padStart(2, "0")}:${year}`,
                title: OWASP_CATEGORY_NAMES[year]?.[category - 1] ?? null,
                url: owaspUrl(year, category),
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

// Filters shared by the overview and the single file view. Each list is an "any of" match and the
// lists combine with AND, e.g. tags=[a, b] + cwes=[89] keeps issues tagged a or b that map to CWE-89.
export const EMPTY_FILTERS = {
    search: "",
    kinds: [],
    severities: [],
    rules: [],
    tags: [],
    cwes: [],
    owasp: [],
};

export const FILTER_LIST_KEYS = ["kinds", "severities", "rules", "tags", "cwes", "owasp"];

export const countActiveFilters = (filters, keys = FILTER_LIST_KEYS) =>
    (filters.search.trim() ? 1 : 0) + keys.filter((key) => filters[key].length > 0).length;

const intersects = (selected, values) => selected.length === 0 || values.some((value) => selected.includes(value));

export const matchesSearch = (issue, term) => {
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

export const matchesFilters = (issue, filters) =>
    intersects(filters.kinds, [getRuleKind(issue)])
    && intersects(filters.severities, [getSeverity(issue)])
    && intersects(filters.rules, [issue.ruleID])
    && intersects(filters.tags, issue.tags ?? [])
    && intersects(filters.cwes, getCweList(issue).map(String))
    && intersects(filters.owasp, getOwaspEntries(issue).map(({ code }) => code))
    && matchesSearch(issue, filters.search.trim());

const tally = (map, key, label) => {
    const entry = map.get(key) ?? { value: key, label, count: 0 };
    entry.count += 1;
    map.set(key, entry);
};

// Distinct filter values present in `issues`, with how many issues carry each one.
export const collectFilterOptions = (issues) => {
    const severities = new Map();
    const rules = new Map();
    const tags = new Map();
    const cwes = new Map();
    const owasp = new Map();
    (issues ?? []).forEach((issue) => {
        const severity = getSeverity(issue);
        if (severity) {
            tally(severities, severity, SEVERITIES[severity].label);
        }
        tally(rules, issue.ruleID, `${issue.ruleID} · ${getRuleName(issue)}`);
        (issue.tags ?? []).forEach((tag) => tally(tags, tag, tag));
        getCweList(issue).forEach((cwe) => tally(cwes, String(cwe), cweLabel(cwe)));
        getOwaspEntries(issue).forEach(({ code, title }) => tally(owasp, code, title ? `${code} · ${title}` : code));
    });
    const byLabel = (a, b) => a.label.localeCompare(b.label, undefined, { numeric: true });
    return {
        severities: [...severities.values()].sort((a, b) => SEVERITIES[b.value].rank - SEVERITIES[a.value].rank),
        rules: [...rules.values()].sort(byLabel),
        tags: [...tags.values()].sort(byLabel),
        cwes: [...cwes.values()].sort((a, b) => Number(a.value) - Number(b.value)),
        owasp: [...owasp.values()].sort(byLabel),
    };
};
