import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { getRefreshDelayMs } from "../utils/jwtUtils";

type AuthResponse = {
  success: boolean;
  message: string;
  email?: string;
  username?: string;
  accessToken?: string;
  refreshToken?: string;
  requires2fa?: boolean;
  twoFactorEnabled?: boolean;
  qrCodeUri?: string;
  backupCodes?: string[];
  provider?: string;
  oauthAccount?: boolean;
};

type AuthUser = {
  email: string;
  username: string;
  provider?: string;
  oauthAccount?: boolean;
};

type AuthContextType = {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isGuest: boolean;
  isLoading: boolean;
  continueAsGuest: () => void;
  login: (email: string, password: string) => Promise<AuthResponse>;
  verifyTwoFactorLogin: (email: string, password: string, code: string) => Promise<AuthResponse>;
  register: (email: string, password: string, confirmPassword: string, captchaChallenge?: string) => Promise<AuthResponse>;
  oauthLogin: (provider: string, accessToken: string) => Promise<AuthResponse>;
  forgotPassword: (email: string) => Promise<AuthResponse>;
  resetPassword: (resetToken: string, newPassword: string) => Promise<AuthResponse>;
  setupTwoFactor: (email: string, password: string) => Promise<AuthResponse>;
  verifyTwoFactorSetup: (email: string, password: string, code: string) => Promise<AuthResponse>;
  disableTwoFactor: (email: string, password: string, code: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const GUEST_SESSION_KEY = "estvalgus_guest";

const guestUser = (): AuthUser => ({
  email: "",
  username: "Guest",
});

const clearGuestSession = () => {
  sessionStorage.removeItem(GUEST_SESSION_KEY);
};

const restoreGuestSession = (): AuthUser | null => {
  if (sessionStorage.getItem(GUEST_SESSION_KEY) === "1") {
    return guestUser();
  }
  return null;
};

let accessToken: string | null = null;
let refreshTimer: ReturnType<typeof setTimeout> | null = null;

const clearRefreshTimer = () => {
  if (refreshTimer !== null) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
};

const authHeaders = (): HeadersInit => {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }
  return headers;
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isGuest, setIsGuest] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const isMountedRef = useRef(true);

  const continueAsGuest = useCallback(() => {
    clearGuestSession();
    sessionStorage.setItem(GUEST_SESSION_KEY, "1");
    accessToken = null;
    clearRefreshTimer();
    setIsGuest(true);
    setUser(guestUser());
  }, []);

  const scheduleTokenRefresh = useCallback((token: string) => {
    clearRefreshTimer();

    const delayMs = getRefreshDelayMs(token);
    refreshTimer = setTimeout(async () => {
      try {
        const response = await fetch("/api/auth/refresh", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({}),
        });

        if (!response.ok || !isMountedRef.current) {
          return;
        }

        const data: AuthResponse = await response.json();
        if (data.success && data.accessToken && data.email && data.username) {
          accessToken = data.accessToken;
          setUser({
            email: data.email,
            username: data.username,
            provider: data.provider,
            oauthAccount: data.oauthAccount,
          });
          scheduleTokenRefresh(data.accessToken);
        }
      } catch {
        // Keep current session until the access token naturally expires.
      }
    }, delayMs);
  }, []);

  const persistAuth = useCallback(
    (data: AuthResponse) => {
      if (!data.success || !data.accessToken || !data.email || !data.username) {
        return;
      }

      accessToken = data.accessToken;
      clearGuestSession();
      setIsGuest(false);
      setUser({
        email: data.email,
        username: data.username,
        provider: data.provider,
        oauthAccount: data.oauthAccount,
      });
      scheduleTokenRefresh(data.accessToken);
    },
    [scheduleTokenRefresh]
  );

  const refreshSession = useCallback(async () => {
    try {
      const response = await fetch("/api/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({}),
      });

      if (!response.ok) {
        accessToken = null;
        clearRefreshTimer();
        const guest = restoreGuestSession();
        if (guest) {
          setIsGuest(true);
          setUser(guest);
        } else {
          setIsGuest(false);
          setUser(null);
        }
        return false;
      }

      const data: AuthResponse = await response.json();
      if (data.success && data.accessToken && data.email && data.username) {
        persistAuth(data);
        return true;
      }

      accessToken = null;
      clearRefreshTimer();
      const guest = restoreGuestSession();
      if (guest) {
        setIsGuest(true);
        setUser(guest);
      } else {
        setIsGuest(false);
        setUser(null);
      }
      return false;
    } catch {
      accessToken = null;
      clearRefreshTimer();
      const guest = restoreGuestSession();
      if (guest) {
        setIsGuest(true);
        setUser(guest);
      } else {
        setIsGuest(false);
        setUser(null);
      }
      return false;
    }
  }, [persistAuth]);

  useEffect(() => {
    isMountedRef.current = true;

    const hydrateSession = async () => {
      await refreshSession();
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    };

    hydrateSession();

    return () => {
      isMountedRef.current = false;
      clearRefreshTimer();
    };
  }, [refreshSession]);

  const login = async (email: string, password: string) => {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ email, password }),
    });

    const data: AuthResponse = await response.json();
    if (response.ok && data.success) {
      persistAuth(data);
    }

    return data;
  };

  const verifyTwoFactorLogin = async (email: string, password: string, code: string) => {
    const response = await fetch("/api/auth/2fa/verify-login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ email, password, twoFactorCode: code }),
    });

    const data: AuthResponse = await response.json();
    if (response.ok && data.success) {
      persistAuth(data);
    }

    return data;
  };

  const register = async (
    email: string,
    password: string,
    confirmPassword: string,
    captchaChallenge?: string
  ) => {
    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ email, password, confirmPassword, captchaChallenge }),
    });

    const data: AuthResponse = await response.json();
    if (response.ok && data.success) {
      persistAuth(data);
    }

    return data;
  };

  const oauthLogin = async (provider: string, token: string) => {
    const response = await fetch("/api/auth/oauth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ provider, accessToken: token }),
    });

    const raw = await response.text();
    let data: AuthResponse;
    try {
      data = raw ? JSON.parse(raw) : { success: false, message: "OAuth login failed." };
    } catch {
      return { success: false, message: raw || "OAuth login failed. Please try again." };
    }

    if (response.ok && data.success) {
      persistAuth(data);
    }

    return data;
  };

  const forgotPassword = async (email: string) => {
    const response = await fetch("/api/auth/forgot-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
    });
    return response.json();
  };

  const resetPassword = async (resetToken: string, newPassword: string) => {
    const response = await fetch("/api/auth/reset-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ resetToken, newPassword }),
    });
    return response.json();
  };

  const setupTwoFactor = async (email: string, password: string) => {
    const response = await fetch("/api/auth/2fa/setup", {
      method: "POST",
      headers: authHeaders(),
      credentials: "include",
      body: JSON.stringify({ email, password }),
    });
    return response.json();
  };

  const verifyTwoFactorSetup = async (email: string, password: string, code: string) => {
    const response = await fetch("/api/auth/2fa/verify-setup", {
      method: "POST",
      headers: authHeaders(),
      credentials: "include",
      body: JSON.stringify({ email, password, twoFactorCode: code }),
    });
    return response.json();
  };

  const disableTwoFactor = async (email: string, password: string, code: string) => {
    const response = await fetch("/api/auth/2fa/disable", {
      method: "POST",
      headers: authHeaders(),
      credentials: "include",
      body: JSON.stringify({ email, password, twoFactorCode: code }),
    });
    return response.json();
  };

  const logout = async () => {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: authHeaders(),
        credentials: "include",
      });
    } catch (error) {
      console.error("Logout error:", error);
    } finally {
      accessToken = null;
      clearGuestSession();
      setIsGuest(false);
      setUser(null);
      clearRefreshTimer();
    }
  };

  const value = useMemo(
    () => ({
      user,
      token: accessToken,
      isAuthenticated: !!accessToken,
      isGuest,
      isLoading,
      continueAsGuest,
      login,
      verifyTwoFactorLogin,
      register,
      oauthLogin,
      forgotPassword,
      resetPassword,
      setupTwoFactor,
      verifyTwoFactorSetup,
      disableTwoFactor,
      logout,
      refreshSession,
    }),
    [user, isGuest, isLoading, continueAsGuest, persistAuth, refreshSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
};
