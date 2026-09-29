import Header from "./components/Header";
import { useEffect, useMemo, useState } from "react";
import { Box } from "@mui/material";
import MainView from "./components/MainView";
import SingleFileView from "./components/SingleFileView";
import useHashRoute from "./useHashRoute";
import { EMPTY_FILTERS, getFileKey } from "./issueMeta";
import { PROJECT_KINDS, getBaseName } from "./projectTree";

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
  // Shared by both views so filters picked on the overview still apply after opening a file.
  const [filters, setFilters] = useState(EMPTY_FILTERS)

  const requestedFile = route.view === "file"
    ? analysisResults.scannedFiles?.find((scannedFile) => getFileKey(scannedFile) === route.fileKey)
    : undefined

  useEffect(() => {
    const { projectName, projectVersion } = analysisResults
    if (requestedFile) {
      document.title = `${getBaseName(requestedFile)} · ${projectName ?? "Scan Report"}`
    } else if (projectName) {
      const project = projectVersion ? `${projectName} v${projectVersion}` : projectName
      document.title = `${project} · Ballerina Scan Report`
    } else {
      document.title = "Ballerina Scan Report"
    }
  }, [requestedFile, analysisResults])

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "var(--page-background)" }}>
      <Header
        projectName={analysisResults.projectName}
        projectVersion={analysisResults.projectVersion}
        projectKind={analysisResults.projectKind}
        packageCount={analysisResults.packages?.length ?? 0}
        onHome={openMain}
      />
      <Box component="main" sx={{ maxWidth: "1280px", margin: "0 auto", padding: { xs: "1rem", md: "1.5rem 2rem 3rem" } }}>
        {requestedFile ?
          <SingleFileView
            key={route.fileKey}
            requestedFile={requestedFile}
            project={analysisResults}
            selectedIssue={route.issueIndex}
            onBack={openMain}
            onSelectIssue={(issueIndex) => selectIssue(route.fileKey, issueIndex)}
            filters={filters}
            onFiltersChange={setFilters}
          /> :
          <MainView
            analyzedFiles={analysisResults.scannedFiles}
            packages={analysisResults.packages}
            workspace={analysisResults.projectKind === PROJECT_KINDS.WORKSPACE}
            onOpenFile={(file) => openFile(getFileKey(file))}
            missingFile={route.view === "file" ? route.fileKey : null}
            filters={filters}
            onFiltersChange={setFilters}
          />
        }
      </Box>
    </Box>
  );
}

export default App
