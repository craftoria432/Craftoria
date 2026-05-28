// src/App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { Toaster } from 'react-hot-toast';
import theme from './theme';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import Layout from './components/layout/Layout';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import SellerVerification from './pages/SellerVerification';
import SellerVerificationDashboard from './pages/SellerVerificationDashboard';
import ProductManagement from './pages/ProductManagement';
import UserManagement from './pages/UserManagement';
import OrderOversight from './pages/OrderOversight';
import CoSellerStores from './pages/CoSellerStores';
import LearningResources from './pages/LearningResources';
import Reports from './pages/Reports';
import Settings from './pages/Settings';

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<Login />} />

            {/* Protected Routes - Wrapped in Layout */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              {/* Nested routes inside Layout */}
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="sellers" element={<SellerVerification />} />
              <Route path="seller-verification" element={<SellerVerificationDashboard />} />
              <Route path="products" element={<ProductManagement />} />
              <Route path="users" element={<UserManagement />} />
              <Route path="orders" element={<OrderOversight />} />
              <Route path="co-seller-stores" element={<CoSellerStores />} />
              <Route path="learning-resources" element={<LearningResources />} />
              <Route path="reports" element={<Reports />} />
              <Route path="settings" element={<Settings />} />
            </Route>

            {/* Catch all - redirect to dashboard */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Router>
      </AuthProvider>

      {/* Toast Notifications */}
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3000,
          style: {
            borderRadius: '10px',
            background: '#333',
            color: '#fff',
            fontSize: '0.85rem',
          },
          success: {
            iconTheme: {
              primary: '#4CAF50',
              secondary: '#fff',
            },
          },
          error: {
            iconTheme: {
              primary: '#f44336',
              secondary: '#fff',
            },
          },
        }}
      />
    </ThemeProvider>
  );
}

export default App;
