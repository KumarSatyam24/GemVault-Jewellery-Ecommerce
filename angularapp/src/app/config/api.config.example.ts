// Rename this file to api.config.ts and update the URLs as needed

const LOCAL_API_URL = 'http://localhost:8080/api';

const PORTAL_API_URL =
  'https://your-project.examly.io/proxy/8080/api';

const isPortal =
  typeof window !== 'undefined' &&
  window.location.hostname.endsWith('premiumproject.examly.io');

export const API_BASE_URL = isPortal ? PORTAL_API_URL : LOCAL_API_URL;
export const API_BASE_URLS = [LOCAL_API_URL, PORTAL_API_URL];