/**
 * main.tsx — React application entry point
 *
 * WHY THIS FILE EXISTS:
 * This is the JavaScript equivalent of web.xml in the Spring MVC project you know.
 * It bootstraps the entire React application into the HTML page. Every import,
 * every component, every API call traces back to this single starting point.
 *
 * HOW IT WORKS:
 * index.html (in frontend/) has:  <div id="root"></div>
 * This file finds that element and mounts the <App /> component tree inside it.
 * After createRoot().render(), React "owns" the DOM inside #root and manages
 * all subsequent updates — you never touch the DOM directly in React.
 *
 * STRICTMODE:
 * <StrictMode> is a React development tool that intentionally renders components
 * TWICE to surface bugs caused by side effects in the render function. You may
 * notice your useEffect runs twice on initial mount in dev mode — this is intentional
 * (it tests that your cleanup functions work correctly). StrictMode has zero effect
 * in production builds.
 *
 * DEPENDS ON: react, react-dom/client, App.tsx, app.css
 * DEPENDED ON BY: index.html (via <script type="module" src="/src/main.tsx">)
 */

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

// Import the single global stylesheet FIRST so base styles apply to everything.
// All CSS for the entire app lives in app.css — no separate per-component CSS files.
import './app.css';

import App from './App.tsx';

// ---- Mount React into the DOM ----

/**
 * document.getElementById('root') finds <div id="root"> in index.html.
 * The `!` non-null assertion tells TypeScript "I'm certain this won't be null."
 * Without `!`, TypeScript would warn that getElementById might return null — which
 * is true in general, but we know our index.html always has the root div.
 *
 * createRoot() is the React 18 mounting API. It enables Concurrent Mode features
 * like automatic batching: multiple setState calls in one event handler trigger
 * only ONE re-render instead of one per setState. This is a free performance win.
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
