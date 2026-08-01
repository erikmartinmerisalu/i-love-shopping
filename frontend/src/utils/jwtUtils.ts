const REFRESH_BUFFER_MS = 60_000;

export const getTokenExpiryMs = (token: string): number | null => {
  try {
    const payload = token.split(".")[1];
    if (!payload) {
      return null;
    }

    const decoded = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return typeof decoded.exp === "number" ? decoded.exp * 1000 : null;
  } catch {
    return null;
  }
};

export const getRefreshDelayMs = (token: string, fallbackMs = 14 * 60 * 1000): number => {
  const expiryMs = getTokenExpiryMs(token);
  if (!expiryMs) {
    return fallbackMs;
  }

  const delay = expiryMs - Date.now() - REFRESH_BUFFER_MS;
  return Math.max(delay, 30_000);
};
