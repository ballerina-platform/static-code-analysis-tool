import { Alert, Box, Typography } from "@mui/material";
import InfoCards from "./InfoCards";
import MainTable from "./MainTable";
import { useMemo } from "react";
import { countActiveFilters, countByKind, countSuppressed, isSuppressed, matchesFilters, matchesStatus } from "../issueMeta";

function retrieveAllStats(analyzedFiles) {
    const issues = (analyzedFiles ?? []).flatMap((analyzedFile) => analyzedFile.issues ?? [])
    return {
        filesScanned: (analyzedFiles ?? []).length,
        kindCounts: countByKind(issues.filter((issue) => !isSuppressed(issue))),
        suppressed: countSuppressed(issues),
    }
}

// Per-file counts only include issues matching the filters; with filters active, files with no
// matching issue drop out so the table lists exactly the files worth opening.
function buildFileRecords(analyzedFiles, filters) {
    const filtering = countActiveFilters(filters) > 0
    return (analyzedFiles ?? []).flatMap((analyzedFile) => {
        const matching = (analyzedFile.issues ?? []).filter((issue) => matchesFilters(issue, filters))
        if (filtering && matching.length === 0) {
            return []
        }
        const counts = countByKind(matching)
        return [{
            file: analyzedFile,
            fileName: analyzedFile.fileName,
            filePath: analyzedFile.filePath,
            codeSmells: counts.CODE_SMELL,
            bugs: counts.BUG,
            vulnerabilities: counts.VULNERABILITY,
            totalIssues: matching.length,
        }]
    })
}

function MainView({ analyzedFiles, packages, workspace, onOpenFile, missingFile, filters, onFiltersChange }) {
    const statistics = useMemo(() => retrieveAllStats(analyzedFiles), [analyzedFiles])
    const allIssues = useMemo(() => (analyzedFiles ?? []).flatMap((file) => file.issues ?? []), [analyzedFiles])
    const fileRecords = useMemo(() => buildFileRecords(analyzedFiles, filters), [analyzedFiles, filters])
    // The kind chips and the filter dropdowns offer only what the selected status lists.
    const statusIssues = useMemo(
        () => allIssues.filter((issue) => matchesStatus(issue, filters.status)), [allIssues, filters.status])
    const kindCounts = useMemo(() => countByKind(statusIssues), [statusIssues])

    return (
        <Box sx={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
            {missingFile &&
                <Alert severity="warning" variant="outlined" sx={{ bgcolor: "#ffffff" }}>
                    The file <strong>{missingFile}</strong> isn't part of this report. Showing the overview instead.
                </Alert>
            }
            <InfoCards statistics={statistics} />
            {analyzedFiles === undefined || analyzedFiles.length === 0 ?
                <ScanReportUnavailableView /> :
                <MainTable
                    onOpenFile={onOpenFile}
                    fileRecords={fileRecords}
                    allIssues={allIssues}
                    statusIssues={statusIssues}
                    kindCounts={kindCounts}
                    filters={filters}
                    onFiltersChange={onFiltersChange}
                    packages={packages}
                    workspace={workspace}
                />
            }
        </Box>
    )
}

const ScanReportUnavailableView = () => {
    return (
        <Box sx={{
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            padding: "3rem 1rem",
            gap: "0.5rem",
            bgcolor: "#ffffff",
            borderRadius: "0.75rem",
            border: "1px dashed var(--primary-color)",
            textAlign: "center",
        }}>
            <Typography
                variant="h3"
                fontWeight="bold"
            >
                Scan Report Unavailable
            </Typography>
            <Typography
                variant="h4"
            >
                Run
                <code style={{ color: "var(--primary-color)" }}> bal scan --scan-report </code>
                to generate a report
            </Typography>
        </Box>
    )
}

export default MainView;
