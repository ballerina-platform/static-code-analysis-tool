import {
    AssignmentOutlined,
    BugReportOutlined,
    CodeOffOutlined,
    LockOpenOutlined
} from "@mui/icons-material"
import {
    Box,
    Card,
    IconButton,
    Typography
} from "@mui/material"

function InfoCards({ statistics }) {
    return (
        <Box sx={{
            display: "flex",
            flexWrap: "wrap",
            justifyContent: "center",
            alignItems: "center",
            gap: "0.5rem",
        }}>
            <Card sx={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                padding: "0.5rem",
                bgcolor: "#2C2C2C",
                width: "15rem",
                height: "100%",
                color: "#ffffff",
                borderRadius: "0.5rem"
            }}>
                <IconButton disabled sx={{ flex: "0.5" }}>
                    <AssignmentOutlined color="white" sx={{ fontSize: "4rem" }} />
                </IconButton>
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    flex: "1"
                }}>
                    <Typography variant="h2">{statistics.filesScanned}</Typography>
                    <Typography variant="h5">Total files scanned</Typography>
                </Box>
            </Card>
            <Card sx={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                padding: "0.5rem",
                bgcolor: "#33B4AF",
                width: "15rem",
                height: "100%",
                color: "#ffffff",
                borderRadius: "0.5rem"
            }}>
                <IconButton disabled sx={{ flex: "0.5" }}>
                    <CodeOffOutlined color="white" sx={{ fontSize: "4rem" }} />
                </IconButton>
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    flex: "1"
                }}>
                    <Typography variant="h2">{statistics.totalCodeSmells}</Typography>
                    <Typography variant="h5">Code Smells</Typography>
                </Box>
            </Card>
            <Card sx={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                padding: "0.5rem",
                bgcolor: "#FEAC39",
                width: "15rem",
                height: "100%",
                color: "#ffffff",
                borderRadius: "0.5rem"
            }}>
                <IconButton disabled sx={{ flex: "0.5" }}>
                    <BugReportOutlined color="white" sx={{ fontSize: "4rem" }} />
                </IconButton>
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    flex: "1"
                }}>
                    <Typography variant="h2">{statistics.totalBugs}</Typography>
                    <Typography variant="h5">Bugs</Typography>
                </Box>
            </Card>
            <Card sx={{
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                padding: "0.5rem",
                bgcolor: "#F74B5A",
                width: "15rem",
                height: "100%",
                color: "#ffffff",
                borderRadius: "0.5rem"
            }}>
                <IconButton disabled sx={{ flex: "0.5" }}>
                    <LockOpenOutlined color="white" sx={{ fontSize: "4rem" }} />
                </IconButton>
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    flex: "1"
                }}>
                    <Typography variant="h2">{statistics.totalVulnerabilities}</Typography>
                    <Typography variant="h5">Vulnerabilities</Typography>
                </Box>
            </Card>
        </Box>
    )
}

export default InfoCards
