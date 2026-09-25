import { AssignmentOutlined } from "@mui/icons-material"
import {
    Box,
    Card,
    Typography
} from "@mui/material"
import { RULE_KINDS, RULE_KIND_ORDER } from "../issueMeta"

const StatCard = ({ value, label, color, Icon }) => (
    <Card elevation={0} sx={{
        display: "flex",
        alignItems: "center",
        gap: "1rem",
        padding: "1rem 1.25rem",
        bgcolor: color,
        color: "#ffffff",
        borderRadius: "0.75rem",
        boxShadow: "0 2px 10px rgba(0, 0, 0, 0.08)",
    }}>
        <Box sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            width: "3.5rem",
            height: "3.5rem",
            borderRadius: "0.75rem",
            bgcolor: "rgba(255, 255, 255, 0.18)",
            flexShrink: 0,
        }}>
            <Icon sx={{ fontSize: "2.2rem", color: "#ffffff" }} />
        </Box>
        <Box>
            <Typography variant="h2" fontWeight="bold" lineHeight={1.1}>{value}</Typography>
            <Typography variant="h5" sx={{ opacity: 0.92 }}>{label}</Typography>
        </Box>
    </Card>
)

function InfoCards({ statistics }) {
    return (
        <Box sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", md: "repeat(4, 1fr)" },
            gap: "1rem",
        }}>
            <StatCard
                value={statistics.filesScanned}
                label="Total files scanned"
                color="#2C2C2C"
                Icon={AssignmentOutlined}
            />
            {RULE_KIND_ORDER.map((kind) => (
                <StatCard
                    key={kind}
                    value={statistics.kindCounts[kind]}
                    label={RULE_KINDS[kind].plural}
                    color={RULE_KINDS[kind].color}
                    Icon={RULE_KINDS[kind].Icon}
                />
            ))}
        </Box>
    )
}

export default InfoCards
