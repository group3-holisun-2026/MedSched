import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';

function App() {
  return (
    <BrowserRouter>
      {/* Bara de navigație simplă */}
      <nav style={{ padding: '15px', background: '#2c3e50', marginBottom: '20px' }}>
        <Link to="/login" style={{ marginRight: '20px', color: 'white', textDecoration: 'none' }}>Login</Link>
        <Link to="/dashboard" style={{ color: 'white', textDecoration: 'none' }}>Dashboard</Link>
      </nav>

      {/* Rutele care schimbă ecranele */}
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;