import Header from "./components/Header";
import { useEffect, useMemo } from "react";
import { Box } from "@mui/material";
import MainView from "./components/MainView";
import SingleFileView from "./components/SingleFileView";
import useHashRoute from "./useHashRoute";
import { getFileKey } from "./issueMeta";

// Get the static analysis data from the Populated DOM
function readScanData() {
  try {
    return JSON.parse(document.getElementById("scanData").innerHTML)
  } catch (e) {
    console.log(e)
    console.log("No static analysis data found!")
    return {}
  }
}

function App() {
  const analysisResults = useMemo(readScanData, [])
  const { route, openFile, openMain, selectIssue } = useHashRoute()

  const requestedFile = route.view === "file"
    ? analysisResults.scannedFiles?.find((scannedFile) => getFileKey(scannedFile) === route.fileKey)
    : undefined

  useEffect(() => {
    document.title = requestedFile
      ? `${requestedFile.fileName} · ${analysisResults.projectName ?? "Scan Report"}`
      : `${analysisResults.projectName ? `${analysisResults.projectName} · ` : ""}Ballerina Scan Report`
  }, [requestedFile, analysisResults.projectName])

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "var(--page-background)" }}>
      <Header projectName={analysisResults.projectName} onHome={openMain} />
      <Box component="main" sx={{ maxWidth: "1280px", margin: "0 auto", padding: { xs: "1rem", md: "1.5rem 2rem 3rem" } }}>
        {requestedFile ?
          <SingleFileView
            key={route.fileKey}
            requestedFile={requestedFile}
            selectedIssue={route.issueIndex}
            onBack={openMain}
            onSelectIssue={(issueIndex) => selectIssue(route.fileKey, issueIndex)}
          /> :
          <MainView
            analyzedFiles={analysisResults.scannedFiles}
            onOpenFile={(file) => openFile(getFileKey(file))}
            missingFile={route.view === "file" ? route.fileKey : null}
          />
        }
      </Box>
    </Box>
  );
}

export default App
