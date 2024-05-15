import Header from "./components/Header";
import { useState } from "react";
import MainView from "./components/MainView";
import SingleFileView from "./components/SingleFileView";


function App() {
  const [fileView, setFileView] = useState({
    viewToggle: false,
    requestedFile: {},
  })


  // Get the static analysis data from the Populated DOM
  let scanData = null
  try {
    scanData = JSON.parse(document.getElementById("scanData").innerHTML)
  } catch (e) {
    console.log(e)
    console.log("No static analysis data found!")
  }
  const analysisResults = scanData !== null ? scanData : {}

  function toggleSingleFileView(viewFile, fileName) {
    // Perform filtering and recieve the object with the file contents
    const requestedFile = analysisResults.scannedFiles.filter((scannedFile) => {
      return scannedFile.fileName === fileName
    })[0]

    setFileView({
      viewToggle: viewFile,
      requestedFile: requestedFile,
    })
  }

  function toggleMainView() {
    setFileView({
      viewToggle: false,
      requestedFile: {}
    })
  }

  return (
    <>
      <Header projectName={analysisResults.projectName} />
      {fileView.viewToggle ?
        <SingleFileView toggleMainView={toggleMainView} requestedFile={fileView.requestedFile} /> :
        <MainView toggleSingleFileView={toggleSingleFileView} analyzedFiles={analysisResults.scannedFiles} />
      }
    </>
  );
}

export default App