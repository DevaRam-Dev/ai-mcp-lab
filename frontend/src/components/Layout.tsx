/**
 * Layout.tsx — Persistent application shell with top navigation bar
 *
 * WHY THIS FILE EXISTS:
 * Every page (Employees, Departments) shares the same top navigation bar.
 * Putting the nav here means we write it once and it appears automatically on
 * every route — we don't paste it at the top of EmployeesPage AND DepartmentsPage.
 *
 * HOW IT WORKS WITH REACT ROUTER:
 * In App.tsx, Layout is the "parent route" element that wraps all child routes.
 * React Router's <Outlet> is a placeholder — it renders whichever child component
 * matches the current URL:
 *   /employees  → <Outlet> renders <EmployeesPage>
 *   /departments → <Outlet> renders <DepartmentsPage>
 *
 * This is the exact same concept as Apache Tiles in the Spring MVC project you know:
 *   Apache Tiles: tiles-definitions.xml puts <tiles:insertAttribute name="content"> in the template
 *   React Router: Layout.tsx puts <Outlet /> in the same position
 *
 * DEPENDS ON: react-router-dom (NavLink, Outlet)
 * DEPENDED ON BY: App.tsx (used as the parent route element)
 */

import { NavLink, Outlet } from 'react-router-dom';

// ---- Component ----

/**
 * Layout renders the nav bar above all page content.
 * No props needed — React Router injects child content automatically via <Outlet>.
 */
function Layout() {
  return (
    <div className="appContainer">

      {/* ---- Top Navigation Bar ---- */}
      <nav className="topNav">

        {/* Brand / Logo area — acts as a visual anchor */}
        <div className="navBrand">
          <span className="navBrandText">AI MCP Lab</span>
        </div>

        {/* Navigation links */}
        <div className="navLinks">
          {/*
            NavLink is React Router's enhanced <a> tag. It works like a normal link
            but automatically knows whether its URL matches the current browser URL.
            We use this to conditionally apply the 'navLinkActive' CSS class so the
            current page's nav item gets a visual highlight (bold + underline).

            The `className` prop accepts a function that receives `{ isActive }` —
            a boolean that React Router sets to true when the link's URL matches
            the current page URL. This is more flexible than CSS :active because
            CSS :active only fires while the mouse button is held down.
          */}
          <NavLink
            to="/employees"
            className={({ isActive }) =>
              isActive ? 'navLink navLinkActive' : 'navLink'
            }
          >
            Employees
          </NavLink>

          <NavLink
            to="/departments"
            className={({ isActive }) =>
              isActive ? 'navLink navLinkActive' : 'navLink'
            }
          >
            Departments
          </NavLink>
        </div>

      </nav>

      {/* ---- Page Content Area ---- */}
      {/*
        <Outlet> is the "slot" where React Router renders the active child component.
        Layout doesn't know or care WHICH page is shown — it just provides the shell.
        Compare to Apache Tiles: this line is equivalent to:
          <tiles:insertAttribute name="content" />
      */}
      <main className="pageContent">
        <Outlet />
      </main>

    </div>
  );
}

export default Layout;
