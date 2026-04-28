/**
 * App.tsx — Root component; defines URL-to-component routing
 *
 * WHY THIS FILE EXISTS:
 * This is the top-level component of the React app. Its only job is to declare
 * which URL paths map to which page components — the equivalent of @RequestMapping
 * annotations in Spring MVC controllers, but declarative in JSX.
 *
 * HOW REACT ROUTER COMPARES TO SPRING MVC ROUTING:
 *   Spring MVC:  @RequestMapping("/employees") on a controller method
 *   React Router: <Route path="employees" element={<EmployeesPage />} />
 *
 * LAYOUT WRAPPING:
 * The Layout component is the "parent" route — it renders the nav bar once and
 * uses <Outlet> to render the active child page below it. This is equivalent to
 * Apache Tiles in the Spring MVC project: one layout template, many page bodies.
 *
 * DEPENDS ON: react-router-dom, Layout.tsx, EmployeesPage.tsx, DepartmentsPage.tsx
 * DEPENDED ON BY: main.tsx (renders <App /> as the root of the React tree)
 */

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import EmployeesPage from './components/EmployeesPage';
import DepartmentsPage from './components/DepartmentsPage';

// ---- App Component ----

function App() {
  return (
    /**
     * BrowserRouter uses the HTML5 History API (window.history.pushState) for
     * client-side navigation. URLs look like /employees rather than /#/employees.
     *
     * All routing components (NavLink, useNavigate, Route) must be inside BrowserRouter —
     * they will throw errors if used outside of a Router context.
     */
    <BrowserRouter>
      {/*
        <Routes> is the container for all route definitions.
        It renders the FIRST <Route> that matches the current URL — only one route
        renders at a time.
      */}
      <Routes>
        {/*
          The parent route uses Layout as its element. Layout renders the nav bar
          and an <Outlet> placeholder. React Router fills <Outlet> with whichever
          child route matches the current URL.
        */}
        <Route path="/" element={<Layout />}>

          {/*
            `index` marks this as the default child route for exactly "/".
            When the user visits http://localhost:5173, React Router matches this
            and renders <Navigate to="/employees" replace />.

            `replace` means the redirect replaces the current history entry — the
            Back button won't loop back to "/" and redirect again.
          */}
          <Route index element={<Navigate to="/employees" replace />} />

          {/*
            path="employees" is relative to the parent "/", so the full URL is /employees.
            React Router renders <EmployeesPage> inside Layout's <Outlet>.
          */}
          <Route path="employees" element={<EmployeesPage />} />

          {/* Full URL: /departments */}
          <Route path="departments" element={<DepartmentsPage />} />

        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
