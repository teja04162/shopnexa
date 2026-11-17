import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api, { getErrorMessage } from "../services/api.js";
import { useAuth } from "../context/AuthContext.jsx";

export default function Login() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const response = await api.post("/api/auth/login", { email, password });
      signIn(response.data);
      navigate(location.state?.from?.pathname ?? "/cart", { replace: true });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page auth-page--login">
      <div className="auth-showcase">
        <Link className="auth-showcase-brand" to="/" aria-label="ShopNexa home">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>ShopNexa</span>
        </Link>
        <div className="auth-showcase-copy">
          <p className="auth-showcase-kicker"><span /> LITTLE THINGS. BETTER EVERYDAY.</p>
          <h2>Good finds<br />feel like <em>you.</em></h2>
          <p>Sign in to pick up where you left off, with your saved finds and everyday favorites close at hand.</p>
          <div className="auth-showcase-perks">
            <span><b aria-hidden="true">✓</b> Your wishlist, always near</span>
            <span><b aria-hidden="true">✓</b> A smoother way to shop</span>
          </div>
        </div>
        <div className="auth-showcase-art" aria-hidden="true">
          <span className="auth-art-sun" />
          <span className="auth-art-ring auth-art-ring--one" />
          <span className="auth-art-ring auth-art-ring--two" />
          <span className="auth-art-plinth" />
          <span className="auth-art-object auth-art-object--one" />
          <span className="auth-art-object auth-art-object--two" />
          <span className="auth-art-object auth-art-object--three" />
          <span className="auth-art-caption">CURATED FOR YOUR EVERYDAY</span>
        </div>
      </div>

      <div className="auth-form-side">
        <form className={`auth-card auth-card--login${error ? " auth-card--has-error" : ""}`} onSubmit={submit}>
          <p className="eyebrow">WELCOME BACK</p>
          <h1>Sign in to ShopNexa</h1>
          <p className="auth-intro">Your next great find is just around the corner.</p>
          {location.state?.sessionExpired && (
            <div className="notice notice--error" role="status">Your session expired. Please sign in again to continue.</div>
          )}
          {error && <div className="notice notice--error auth-error" role="alert">{error}</div>}
          <label className="auth-field" htmlFor="login-email">
            Email address
            <span className="auth-input-wrap">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3.5" y="5" width="17" height="14" rx="2" />
                <path d="m4.5 7 7.5 6 7.5-6" />
              </svg>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </span>
          </label>
          <label className="auth-field" htmlFor="login-password">
            Password
            <span className="auth-input-wrap">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="4.5" y="10" width="15" height="11" rx="2" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3m-4 5v2" />
              </svg>
              <input
                id="login-password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                placeholder="Enter your password"
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <button
                className="password-visibility"
                type="button"
                aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword}
                onClick={() => setShowPassword((visible) => !visible)}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </span>
          </label>
          <button className="button button--primary auth-submit auth-submit--login" type="submit" disabled={submitting}>
            {submitting ? <><span className="button-spinner" aria-hidden="true" /> Signing you in…</> : <>Sign in <span aria-hidden="true">→</span></>}
          </button>
          <p className="auth-switch">New to ShopNexa? <Link to="/register">Create an account <span aria-hidden="true">→</span></Link></p>
          <p className="auth-secure-note"><span aria-hidden="true">▣</span> Your account details are protected.</p>
        </form>
      </div>
    </section>
  );
}
