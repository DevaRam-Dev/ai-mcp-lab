/**
 * client.ts — Shared Axios HTTP client instance
 *
 * WHY THIS FILE EXISTS:
 * Every API module (employees.ts, departments.ts) needs the same base URL and
 * default headers. Defining one Axios instance here means:
 *   - We change the server URL in ONE place, not in every fetch call
 *   - We add auth headers or request interceptors in ONE place later if needed
 *   - Components never import axios directly — they import typed API functions instead
 *
 * WHAT IS AXIOS:
 * Axios is a promise-based HTTP client, similar to the browser's built-in fetch()
 * but with improvements: automatic JSON parsing of responses, better error objects
 * that include the server's HTTP status code, and a cleaner API for setting headers.
 *
 * CROSS-ORIGIN (CORS) NOTE:
 * The React dev server runs on port 5173; Spring Boot runs on port 8080.
 * Different ports = different "origins" = browsers block the request by default.
 * The Spring Boot controllers have @CrossOrigin(origins = "http://localhost:5173")
 * which tells the browser it's safe to send requests from 5173 to 8080.
 * Without that annotation, every API call here would fail with a CORS error.
 *
 * DEPENDS ON: axios (npm package)
 * DEPENDED ON BY: api/employees.ts, api/departments.ts
 */

import axios from 'axios';

// ---- Create Shared Axios Instance ----

/**
 * axios.create() returns a preconfigured Axios instance.
 * All API functions import this `client` object and call client.get(), client.post(), etc.
 *
 * baseURL: every request path is appended to this — so '/api/employees' becomes
 *          'http://localhost:8080/api/employees' automatically.
 *
 * Content-Type header: tells the server we're sending JSON in POST/PUT request bodies.
 *                      Without it, Spring Boot's @RequestBody deserialization would fail.
 */
const client = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default client;
