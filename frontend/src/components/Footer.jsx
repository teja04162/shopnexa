import React from "react";
import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-callout">
        <div>
          <p className="footer-eyebrow">A LITTLE SOMETHING FOR EVERY DAY</p>
          <h2>Make room for a great find.</h2>
          <p>Explore useful, lovely things for the way you live.</p>
        </div>
        <Link className="footer-shop-link" to="/products">Shop the collection <span aria-hidden="true">→</span></Link>
      </div>

      <div className="footer-main">
        <div className="footer-brand-column">
          <Link className="brand brand--footer" to="/" aria-label="ShopNexa home">
            <span className="brand-mark" aria-hidden="true">S</span>
            <span className="brand-copy">
              <span>ShopNexa</span>
              <small>Everyday, considered.</small>
            </span>
          </Link>
          <p>Thoughtful finds for everyday living. Discover pieces that make the everyday a little better.</p>
        </div>

        <nav className="footer-link-column" aria-label="Shop links">
          <h3>Explore</h3>
          <Link to="/products">Shop all</Link>
          <Link to="/products?sort=newest">New arrivals</Link>
          <Link to="/products?sort=rating">Top rated</Link>
          <Link to="/#shop-categories">Categories</Link>
        </nav>

        <nav className="footer-link-column" aria-label="Account links">
          <h3>Your account</h3>
          <Link to="/login">Sign in</Link>
          <Link to="/register">Create account</Link>
          <Link to="/wishlist">Wishlist</Link>
          <Link to="/cart">Shopping cart</Link>
        </nav>

        <div className="footer-promise-column">
          <h3>The thoughtful details</h3>
          <p><span aria-hidden="true">✓</span> Free delivery over ₹1,000</p>
          <p><span aria-hidden="true">✓</span> Cash on delivery available</p>
          <p><span aria-hidden="true">✓</span> Secure checkout</p>
        </div>
      </div>

      <div className="footer-bottom">
        <span>© {new Date().getFullYear()} ShopNexa</span>
        <span>Made for your next great find.</span>
      </div>
    </footer>
  );
}
