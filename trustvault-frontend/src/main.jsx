import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './Styles/tokens.css'
import './Styles/global.css'
import './Styles/layout.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
