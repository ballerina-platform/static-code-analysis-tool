import {
    Box,
    ButtonBase,
    Typography
} from "@mui/material";
import { Inventory2Outlined, WorkspacesOutlined } from "@mui/icons-material";
import BallerinaLogo from "../resources/Ballerina-Logo";
import { PROJECT_KINDS } from "../projectTree";

const KIND_DISPLAY = {
    [PROJECT_KINDS.WORKSPACE]: { Icon: WorkspacesOutlined, label: "Workspace" },
    [PROJECT_KINDS.PACKAGE]: { Icon: Inventory2Outlined, label: "Package" },
};

function Header({ projectName, projectVersion, projectKind, packageCount, onHome }) {
    const { Icon, label } = KIND_DISPLAY[projectKind] ?? KIND_DISPLAY[PROJECT_KINDS.PACKAGE];
    const workspace = projectKind === PROJECT_KINDS.WORKSPACE;
    return (
        <Box component="header" sx={{
            position: "sticky",
            top: 0,
            zIndex: 10,
            bgcolor: "#ffffff",
            borderBottom: "1px solid var(--primary-color)",
            boxShadow: "0 1px 6px rgba(0, 0, 0, 0.04)",
        }}>
            <Box sx={{
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "0.75rem",
                maxWidth: "1280px",
                margin: "0 auto",
                padding: { xs: "0.9rem 1rem", md: "1rem 2rem" },
            }}>
                <ButtonBase
                    onClick={onHome}
                    aria-label="Go to report overview"
                    sx={{ display: "flex", alignItems: "baseline", gap: "0.5rem", borderRadius: "0.5rem", padding: "0.25rem" }}
                >
                    <BallerinaLogo />
                    <Typography
                        variant="h4"
                        color="primary"
                        fontWeight="bold"
                        fontSize="1.4rem">
                        Scan Report
                    </Typography>
                </ButtonBase>
                {projectName &&
                    <Box sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        padding: "0.35rem 0.85rem",
                        borderRadius: "999px",
                        border: "1px solid var(--surface-border)",
                        bgcolor: "var(--page-background)",
                    }}>
                        <Icon fontSize="small" color="primary" />
                        <Typography variant="caption" fontWeight={700} sx={{
                            textTransform: "uppercase",
                            letterSpacing: "0.04em",
                            color: "primary.main",
                        }}>
                            {label}
                        </Typography>
                        <Typography variant="h5" fontWeight="bold">
                            {projectName}
                        </Typography>
                        {projectVersion &&
                            <Typography variant="body2" color="text.secondary" fontWeight={600}>
                                v{projectVersion}
                            </Typography>
                        }
                        {workspace && packageCount > 0 &&
                            <Typography variant="body2" color="text.secondary" fontWeight={600}>
                                · {packageCount} {packageCount === 1 ? "package" : "packages"}
                            </Typography>
                        }
                    </Box>
                }
            </Box>
        </Box>
    )
}

export default Header;
