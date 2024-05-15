import { ArrowBackOutlined } from "@mui/icons-material"
import {
    Box,
    Typography,
    Button
} from "@mui/material"
import SingleFileTable from "./SingleFileTable"
import SingleFileContent from "./SingleFileContent"


function SingleFileView({ toggleMainView, requestedFile }) {
    return (
        <Box sx={{ margin: "1rem 0" }}>
            <SingleFileBackIcon toggleMainView={toggleMainView} fileName={requestedFile?.fileName} />
            <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                <SingleFileContent issues={requestedFile.issues} fileContent={requestedFile.fileContent} />
                <SingleFileTable issues={requestedFile.issues} />
            </Box>
        </Box>
    )
}

const SingleFileBackIcon = ({ toggleMainView, fileName }) => {
    return (
        <Box sx={{
            display: "flex",
            flexWrap: "wrap",
            alignItems: "center",
            justifyContent: "space-around"
        }}>
            <Box sx={{
                display: "flex",
                alignItems: "center",
                gap: "1rem",
            }}>
                <Button onClick={() => { toggleMainView() }}
                    sx={{
                        padding: "0 1rem",
                        borderRadius: "0.5rem"
                    }}>
                    <ArrowBackOutlined
                        sx={{ fontSize: "4rem" }}
                        color="primary"
                    />
                </Button>
                <Typography variant="h3" fontWeight="bold">
                    {fileName}
                </Typography>
            </Box>

            {/* To position with flex this empty div has been added */}
            <div></div>
        </Box>
    )
}

export default SingleFileView
