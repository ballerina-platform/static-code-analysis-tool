import { DescriptionOutlined, HomeOutlined, Inventory2Outlined, WorkspacesOutlined } from "@mui/icons-material"
import {
    Box,
    Breadcrumbs,
    IconButton,
    Link,
    Tooltip,
    Typography,
    alpha
} from "@mui/material"
import { useState } from "react"
import SingleFileTable from "./SingleFileTable"
import SingleFileContent from "./SingleFileContent"
import { RULE_KINDS, RULE_KIND_ORDER, countByKind } from "../issueMeta"
import { PROJECT_KINDS, findPackage, getBaseName, getRelativePath, packageLabel } from "../projectTree"

function SingleFileView({ requestedFile, project, selectedIssue, onBack, onSelectIssue, filters, onFiltersChange }) {
    const issues = requestedFile.issues ?? []
    const validSelection = selectedIssue !== null && selectedIssue < issues.length ? selectedIssue : null
    // Bumped on every "Show in code" request so repeating it for the same issue scrolls again.
    const [codeFocus, setCodeFocus] = useState({ issueIndex: null, nonce: 0 })

    // Same idea in the other direction: every click on a code highlight asks the table to reveal that row,
    // even when it's already the selected issue (code -> issue -> "Show in code" -> same highlight again).
    // Seeded with the issue from the URL so a deep link scrolls to its row on load.
    const [tableFocus, setTableFocus] = useState(() => ({ issueIndex: validSelection, nonce: 0 }))

    const showInCode = (issueIndex) => {
        setCodeFocus((prev) => ({ issueIndex, nonce: prev.nonce + 1 }))
    }

    const showInTable = (issueIndex) => {
        onSelectIssue(issueIndex)
        setTableFocus((prev) => ({ issueIndex, nonce: prev.nonce + 1 }))
    }

    return (
        <Box sx={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
            <FileHeader file={requestedFile} project={project} issues={issues} onBack={onBack} />
            <SingleFileContent
                issues={issues}
                fileContent={requestedFile.fileContent ?? ""}
                selectedIssue={validSelection}
                focusRequest={codeFocus}
                onSelectIssue={showInTable}
            />
            <SingleFileTable
                issues={issues}
                selectedIssue={validSelection}
                onSelectIssue={(issueIndex) => onSelectIssue(issueIndex === validSelection ? null : issueIndex)}
                fileName={getBaseName(requestedFile)}
                onShowInCode={showInCode}
                focusRequest={tableFocus}
                filters={filters}
                onFiltersChange={onFiltersChange}
            />
        </Box>
    )
}

// In a workspace the package's folder is shown as the package itself, so the trail reads project / package / path.
// Each crumb carries its path so far, which keys it.
const pathCrumbs = (file, pkg, workspace) => {
    const relativePath = getRelativePath(file)
    const inPackage = workspace && pkg?.path && relativePath.startsWith(`${pkg.path}/`)
    const segments = inPackage ? relativePath.slice(pkg.path.length + 1).split("/") : relativePath.split("/")
    const crumbs = inPackage ? [{ name: pkg.name, path: pkg.path }] : []
    let path = inPackage ? pkg.path : ""
    segments.forEach((segment) => {
        path = path ? `${path}/${segment}` : segment
        crumbs.push({ name: segment, path })
    })
    return crumbs
}

const FileHeader = ({ file, project, issues, onBack }) => {
    const counts = countByKind(issues)
    const workspace = project?.projectKind === PROJECT_KINDS.WORKSPACE
    const pkg = findPackage(project?.packages, file)
    const crumbs = pathCrumbs(file, pkg, workspace)
    const relativePath = getRelativePath(file)
    return (
        <Box sx={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.75rem" }}>
                <Tooltip title="Home">
                    <IconButton
                        onClick={onBack}
                        size="small"
                        aria-label="Home"
                        sx={{
                            border: "1px solid var(--surface-border)",
                            bgcolor: "#ffffff",
                            "&:hover": { bgcolor: alpha("#20b6b0", 0.08), borderColor: "var(--primary-color)" },
                        }}
                    >
                        <HomeOutlined fontSize="small" color="primary" />
                    </IconButton>
                </Tooltip>
                <Breadcrumbs aria-label="breadcrumb" sx={{ fontSize: "14px" }}>
                    <Link component="button" underline="hover" color="primary" onClick={onBack} sx={{ fontWeight: 600, fontSize: "14px" }}>
                        {project?.projectName ?? "All files"}
                    </Link>
                    {crumbs.map((crumb, index) => (
                        <Typography key={crumb.path} fontSize="14px"
                            color={index === crumbs.length - 1 ? "text.primary" : "text.secondary"}
                            fontWeight={index === crumbs.length - 1 ? 600 : 400}>
                            {crumb.name}
                        </Typography>
                    ))}
                </Breadcrumbs>
            </Box>

            <Box sx={{
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "1rem",
                padding: "1rem 1.25rem",
                bgcolor: "#ffffff",
                borderRadius: "0.75rem",
                border: "1px solid var(--surface-border)",
            }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: "0.75rem", minWidth: 0 }}>
                    <Box sx={{
                        display: "flex",
                        padding: "0.5rem",
                        borderRadius: "0.6rem",
                        bgcolor: alpha("#20b6b0", 0.1),
                    }}>
                        <DescriptionOutlined color="primary" />
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                        <Typography variant="h4" fontWeight="bold" noWrap>{getBaseName(file)}</Typography>
                        {relativePath !== getBaseName(file) &&
                            <Typography variant="body2" color="text.secondary" noWrap title={file.filePath}
                                sx={{ fontFamily: "consolas, monospace" }}>
                                {relativePath}
                            </Typography>
                        }
                        {pkg && <PackageInfo pkg={pkg} workspaceName={workspace ? project.projectName : null} />}
                    </Box>
                </Box>
                <Box sx={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                    {RULE_KIND_ORDER.map((kind) => {
                        const { Icon, color, label, plural } = RULE_KINDS[kind]
                        return (
                            <Box key={kind} sx={{
                                display: "flex",
                                alignItems: "center",
                                gap: "0.4rem",
                                padding: "0.3rem 0.75rem",
                                borderRadius: "999px",
                                bgcolor: alpha(color, counts[kind] > 0 ? 0.14 : 0.05),
                                opacity: counts[kind] > 0 ? 1 : 0.6,
                            }}>
                                <Icon fontSize="small" sx={{ color }} />
                                <Typography variant="body2" fontWeight={700}>{counts[kind]}</Typography>
                                <Typography variant="body2">{counts[kind] === 1 ? label : plural}</Typography>
                            </Box>
                        )
                    })}
                </Box>
            </Box>
        </Box>
    )
}

const infoChipSx = {
    display: "flex",
    alignItems: "center",
    gap: "0.35rem",
    padding: "0.1rem 0.6rem",
    borderRadius: "999px",
    border: "1px solid var(--surface-border)",
    bgcolor: "var(--page-background)",
}

const PackageInfo = ({ pkg, workspaceName }) => (
    <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.4rem", marginTop: "0.4rem" }}>
        <Box sx={infoChipSx}>
            <Inventory2Outlined sx={{ fontSize: "15px" }} color="primary" />
            <Typography variant="caption" color="text.secondary">Package</Typography>
            <Typography variant="caption" fontWeight={700}>{packageLabel(pkg)}</Typography>
            {pkg.version && <Typography variant="caption" fontWeight={600} color="text.secondary">v{pkg.version}</Typography>}
        </Box>
        {workspaceName &&
            <Box sx={infoChipSx}>
                <WorkspacesOutlined sx={{ fontSize: "15px" }} color="primary" />
                <Typography variant="caption" color="text.secondary">Workspace</Typography>
                <Typography variant="caption" fontWeight={700}>{workspaceName}</Typography>
            </Box>
        }
    </Box>
)

export default SingleFileView
