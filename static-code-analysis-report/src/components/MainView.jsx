import { Alert, Box, Typography } from "@mui/material";
import InfoCards from "./InfoCards";
import MainTable from "./MainTable";
import { useMemo } from "react";
import { countByKind } from "../issueMeta";

function retrieveAllStats(analyzedFiles) {
    const kindCounts = { CODE_SMELL: 0, BUG: 0, VULNERABILITY: 0 }
    const fileRecords = (analyzedFiles ?? []).map((analyzedFile) => {
        const counts = countByKind(analyzedFile.issues)
        Object.keys(kindCounts).forEach((kind) => { kindCounts[kind] += counts[kind] })
        return {
            file: analyzedFile,
            fileName: analyzedFile.fileName,
            filePath: analyzedFile.filePath,
            codeSmells: counts.CODE_SMELL,
            bugs: counts.BUG,
            vulnerabilities: counts.VULNERABILITY,
            totalIssues: analyzedFile.issues?.length ?? 0,
        }
    })

    return {
        filesScanned: fileRecords.length,
        kindCounts,
        fileRecords,
    }
}

function MainView({ analyzedFiles, onOpenFile, missingFile }) {
    const statistics = useMemo(() => retrieveAllStats(analyzedFiles), [analyzedFiles])

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
                    fileRecords={statistics.fileRecords}
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
