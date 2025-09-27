import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { Elements } from '@stripe/react-stripe-js'

import { store } from './store'
import { stripePromise } from './lib/stripe'
import App from './App'
import './index.css'

// Extend window interface for Redux store debugging
declare global {
  interface Window {
    __REDUX_STORE__?: typeof store
  }
}

// Expose store to window for debugging and testing
if (import.meta.env.DEV || import.meta.env.NODE_ENV === 'test') {
  window.__REDUX_STORE__ = store
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <Elements stripe={stripePromise}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </Elements>
    </Provider>
  </React.StrictMode>
)
