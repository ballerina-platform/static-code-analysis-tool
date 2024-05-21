import { Box, Typography } from "@mui/material";
import InfoCards from "./InfoCards";
import MainTable from "./MainTable";
import { useEffect, useState } from "react";

function retrieveSingleFileStats(analyzedFile) {
    let codeSmells = 0
    let bugs = 0
    let vulnerabilities = 0

    if (analyzedFile?.issues.length !== 0) {
        analyzedFile.issues.forEach((issue) => {
            switch (issue.issueSeverity) {
                case "CODE_SMELL":
                    codeSmells += 1
                    break
                case "BUG":
                    bugs += 1
                    break
                case "VULNERABILITY":
                    vulnerabilities += 1
                    break
                default:
                    break
            }
        })
    }

    const singleFileRecord = {
        fileName: analyzedFile.fileName,
        codeSmells: codeSmells,
        bugs: bugs,
        vulnerabilities: vulnerabilities
    }

    return singleFileRecord
}

function retrieveAllStats(analyzedFiles) {
    let filesScanned = 0
    let totalCodeSmells = 0
    let totalBugs = 0
    let totalVulnerabilities = 0
    let singleFileRecords = []

    analyzedFiles?.forEach((analyzedFile) => {
        filesScanned += 1;
        const singleFileRecord = retrieveSingleFileStats(analyzedFile)
        singleFileRecords.push(singleFileRecord)
        totalCodeSmells += singleFileRecord.codeSmells
        totalBugs += singleFileRecord.bugs
        totalVulnerabilities += singleFileRecord.vulnerabilities
    })

    return {
        filesScanned: filesScanned,
        totalCodeSmells: totalCodeSmells,
        totalBugs: totalBugs,
        totalVulnerabilities: totalVulnerabilities,
        singleFileRecords: singleFileRecords
    }
}

function MainView({ toggleSingleFileView, analyzedFiles }) {
    const [statistics, setStatistics] = useState({
        filesScanned: 0,
        totalCodeSmells: 0,
        totalBugs: 0,
        totalVulnerabilities: 0
    })

    const [fileRecords, setFileRecords] = useState([])

    useEffect(() => {
        if (analyzedFiles?.length !== 0) {
            const newStats = retrieveAllStats(analyzedFiles)

            setStatistics({
                filesScanned: newStats.filesScanned,
                totalCodeSmells: newStats.totalCodeSmells,
                totalBugs: newStats.totalBugs,
                totalVulnerabilities: newStats.totalVulnerabilities
            })

            setFileRecords(newStats.singleFileRecords)
        }
    }, [analyzedFiles])

    return (
        <Box sx={{
            marginTop: "1rem",
        }}>
            <InfoCards statistics={statistics} />
            {analyzedFiles === undefined || analyzedFiles.length === 0 ?
                <ScanReportUnavailableView /> :

                <MainTable
                    toggleSingleFileView={toggleSingleFileView}
                    fileRecords={fileRecords}
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
            marginTop: "1rem",
            gap: "0.5rem"
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
