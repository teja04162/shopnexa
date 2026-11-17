import React, { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);

function readSession() {
  try {
    const saved = sessionStorage.getItem("shopnexa-session");
    return saved ? JSON.parse(saved) : null;
  } catch {
    sessionStorage.removeItem("shopnexa-session");
    return null;
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession);

  function signIn(authResponse) {
    const nextSession = {
      accessToken: authResponse.accessToken,
      user: authResponse.user,
    };
    sessionStorage.setItem("shopnexa-session", JSON.stringify(nextSession));
    setSession(nextSession);
  }

  function signOut() {
    sessionStorage.removeItem("shopnexa-session");
    setSession(null);
  }

  const value = useMemo(() => ({
    session,
    user: session?.user ?? null,
    signIn,
    signOut,
  }), [session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
