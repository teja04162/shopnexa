import React from "react";
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import api, { getErrorMessage } from "../services/api.js";

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

export default function ProductCard({ product }) {
  const { user, signOut } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const image = product.imageUrls?.[0];

  function handleActionError(error) {
    if (error.response?.status === 401) {
      signOut();
      navigate("/login", { state: { from: location, sessionExpired: true } });
      return;
    }
    setNotice(getErrorMessage(error));
  }

  async function addToCart() {
    if (!user) {
      navigate("/login", { state: { from: location } });
      return;
    }
    setBusy(true);
    setNotice("");
    try {
      await api.post("/api/cart/items", { productId: product.id, quantity: 1 });
      setNotice("Added to cart");
    } catch (error) {
      handleActionError(error);
    } finally {
      setBusy(false);
    }
  }

  async function saveToWishlist() {
    if (!user) {
      navigate("/login", { state: { from: location } });
      return;
    }
    setBusy(true);
    setNotice("");
    try {
      await api.post(`/api/wishlist/${product.id}`);
      setNotice("Saved to wishlist");
    } catch (error) {
      handleActionError(error);
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="product-card">
      <Link className="product-image-link" to={`/products/${product.id}`}>
        {image ? (
          <img className="product-image" src={image} alt={product.name} loading="lazy" />
        ) : (
          <span className="image-fallback" aria-label="No product image available">SN</span>
        )}
        {product.discountPrice && <span className="product-badge">SPECIAL PRICE</span>}
      </Link>
      <div className="product-info">
        <p className="product-category">{product.categoryName}</p>
        <Link className="product-name" to={`/products/${product.id}`}>{product.name}</Link>
        <div className="product-meta">
          <div className="product-prices">
            <span className="product-price">{currency.format(product.discountPrice ?? product.price)}</span>
            {product.discountPrice && (
              <span className="product-old-price">{currency.format(product.price)}</span>
            )}
          </div>
          <span className="product-rating" aria-label={`${product.ratingAverage} out of 5 stars`}>
            <span aria-hidden="true">★</span> {Number(product.ratingAverage).toFixed(1)}
          </span>
        </div>
        <div className="product-actions">
          <button type="button" disabled={busy || product.stock < 1} onClick={addToCart}>
            {product.stock < 1 ? "Out of stock" : busy ? "Saving..." : "Add to cart"}
          </button>
          <button type="button" className="wishlist-action" aria-label={`Save ${product.name} to wishlist`} disabled={busy} onClick={saveToWishlist}>♡</button>
        </div>
        {notice && <p className="product-action-notice" role="status">{notice}</p>}
      </div>
    </article>
  );
}
