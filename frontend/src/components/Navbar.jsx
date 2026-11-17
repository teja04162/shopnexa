import React from "react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api.js";
import { useAuth } from "../context/AuthContext.jsx";

export default function Navbar() {
  const { user, signOut } = useAuth();
  const [query, setQuery] = useState("");
  const [logoutError, setLogoutError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    if (user) setLogoutError("");
  }, [user]);

  function search(event) {
    event.preventDefault();
    const params = new URLSearchParams();
    if (query.trim()) params.set("q", query.trim());
    navigate(`/products${params.size ? `?${params}` : ""}`);
  }

  async function logout() {
    setLogoutError("");
    try {
      await api.post("/api/auth/logout");
    } catch (error) {
      if (error.response?.status !== 401) {
        setLogoutError("Could not confirm logout with the server. Your local session was cleared.");
      }
    } finally {
      signOut();
      navigate("/");
    }
  }

  return (
    <>
      <div className="site-announcement">
        <span className="announcement-signal" aria-hidden="true" />
        <span>Free delivery on orders above ₹1,000</span>
        <span className="announcement-divider" aria-hidden="true">·</span>
        <span className="announcement-extra">Thoughtful finds, delivered to your door</span>
        <Link to="/products">Shop now <span aria-hidden="true">→</span></Link>
      </div>
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/" aria-label="ShopNexa home">
            <span className="brand-mark" aria-hidden="true">S</span>
            <span className="brand-copy">
              <span>ShopNexa</span>
              <small>Everyday, considered.</small>
            </span>
          </Link>
          <form className="search-form" role="search" onSubmit={search}>
            <label className="sr-only" htmlFor="site-search">Search products</label>
            <input
              id="site-search"
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search products..."
            />
            <button type="submit" aria-label="Search">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="10.8" cy="10.8" r="6.3" />
                <path d="m15.4 15.4 4.1 4.1" />
              </svg>
            </button>
          </form>
          <nav className="account-nav" aria-label="Account navigation">
            {user ? (
              <>
                <Link className="header-action" to="/wishlist" aria-label="Wishlist">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M20.4 8.6c0 5.2-8.4 10.1-8.4 10.1S3.6 13.8 3.6 8.6A4.1 4.1 0 0 1 12 6.7a4.1 4.1 0 0 1 8.4 1.9Z" />
                  </svg>
                  <span className="header-action-label">Wishlist</span>
                </Link>
                <Link className="header-action" to="/cart" aria-label="Shopping cart">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 8h16l-1.2 12H5.2L4 8Z" />
                    <path d="M8.5 9V6a3.5 3.5 0 0 1 7 0v3" />
                  </svg>
                  <span className="header-action-label">Cart</span>
                </Link>
                <button className="header-signout" type="button" onClick={logout}>Sign out</button>
              </>
            ) : (
              <>
                <Link className="header-signin" to="/login">Sign in</Link>
                <Link className="header-join" to="/register">Create account</Link>
              </>
            )}
          </nav>
        </div>
        <div className="header-bottom">
          <nav className="main-nav" aria-label="Shop navigation">
            <Link to="/products">Shop all</Link>
            <Link to="/products?sort=newest">New arrivals</Link>
            <Link to="/products?sort=rating">Top rated</Link>
            <Link to="/#shop-categories">Categories</Link>
          </nav>
          <span className="header-promise">Good finds for everyday living</span>
        </div>
      </header>
      {logoutError && <p className="logout-notice" role="status">{logoutError}</p>}
    </>
  );
}
