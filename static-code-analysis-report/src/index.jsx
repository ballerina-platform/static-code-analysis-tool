import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'
import { ThemeProvider } from '@emotion/react'
import { createTheme } from '@mui/material'

const theme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#20b6b0",
    },
    secondary: {
      main: "#ff7300",
    },
    white: {
      main: "#ffffff",
    }
  },
  typography: {
    h1: {
      fontSize: "38px",
    },
    h2: {
      fontSize: "32px",
    },
    h3: {
      fontSize: "29px",
    },
    h4: {
      fontSize: "21px",
    },
    h5: {
      fontSize: "16px",
    },
    h6: {
      fontSize: "12px",
    },
    p: {
      fontSize: "12px",
    },
  }
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <App />
    </ThemeProvider>
  </React.StrictMode>,
)
