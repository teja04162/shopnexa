import React, { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { getErrorMessage } from "../services/api.js";
import Loading from "../components/Loading.jsx";
import { useAuth } from "../context/AuthContext.jsx";

const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });

export default function Cart() {
  const { signOut } = useAuth();
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [pendingItemIds, setPendingItemIds] = useState(() => new Set());

  const handleRequestError = useCallback((requestError) => {
    if (requestError.response?.status === 401) {
      signOut();
      navigate("/login", {
        replace: true,
        state: { from: { pathname: "/cart" }, sessionExpired: true },
      });
      return;
    }
    setError(getErrorMessage(requestError));
  }, [navigate, signOut]);

  const loadCart = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get("/api/cart");
      setCart(response.data);
      setError("");
    } catch (requestError) {
      handleRequestError(requestError);
    } finally {
      setLoading(false);
    }
  }, [handleRequestError]);

  useEffect(() => { loadCart(); }, [loadCart]);

  function setItemPending(itemId, pending) {
    setPendingItemIds((current) => {
      const next = new Set(current);
      if (pending) next.add(itemId);
      else next.delete(itemId);
      return next;
    });
  }

  async function updateQuantity(item, quantity) {
    if (quantity < 1 || quantity > item.availableStock || pendingItemIds.has(item.id)) return;
    setItemPending(item.id, true);
    setError("");
    try {
      const response = await api.put(`/api/cart/items/${item.id}`, { quantity });
      setCart(response.data);
    } catch (requestError) {
      handleRequestError(requestError);
    } finally {
      setItemPending(item.id, false);
    }
  }

  async function removeItem(itemId) {
    if (pendingItemIds.has(itemId)) return;
    setItemPending(itemId, true);
    setError("");
    try {
      const response = await api.delete(`/api/cart/items/${itemId}`);
      setCart(response.data);
    } catch (requestError) {
      handleRequestError(requestError);
    } finally {
      setItemPending(itemId, false);
    }
  }

  if (loading) {
    return <section className="page-shell detail-loading cart-loading"><Loading label="Gathering your bag..." /></section>;
  }

  if (!cart && error) {
    return (
      <section className="page-shell cart-page">
        <p className="eyebrow">YOUR BAG</p>
        <h1 className="page-title">Shopping cart</h1>
        <div className="cart-load-error">
          <div className="notice notice--error" role="alert">{error}</div>
          <button className="button button--primary" type="button" onClick={loadCart}>Try again</button>
        </div>
      </section>
    );
  }

  const itemCount = cart?.itemCount ?? 0;

  return (
    <section className="page-shell cart-page">
      <div className="cart-heading">
        <div>
          <p className="eyebrow">A GOOD PLACE TO START</p>
          <h1 className="page-title">Your bag<span className="cart-count">{itemCount}</span></h1>
          <p className="cart-intro">The things you picked, all in one place.</p>
        </div>
        <Link className="cart-continue-top" to="/products">
          <span aria-hidden="true">←</span> Keep exploring
        </Link>
      </div>

      {error && <div className="notice notice--error cart-error" role="alert">{error}</div>}

      {!cart?.items?.length ? (
        <div className="cart-empty">
          <div className="cart-empty-art" aria-hidden="true">
            <span className="cart-empty-orbit cart-empty-orbit--one" />
            <span className="cart-empty-orbit cart-empty-orbit--two" />
            <svg viewBox="0 0 64 64">
              <path d="M13 22h38l-3 32H16l-3-32Z" />
              <path d="M23 23v-6a9 9 0 0 1 18 0v6" />
              <path d="M25 35c4 4 10 4 14 0" />
            </svg>
          </div>
          <p className="eyebrow">NOTHING IN HERE YET</p>
          <h2>Your next great find is waiting.</h2>
          <p>Browse the collection and add something lovely to your bag.</p>
          <Link className="button button--primary cart-empty-button" to="/products">
            Discover the collection <span aria-hidden="true">→</span>
          </Link>
        </div>
      ) : (
        <>
          <div className="cart-benefits" aria-label="Shopping benefits">
            <span><b aria-hidden="true">✓</b> Secure checkout</span>
            <span><b aria-hidden="true">✓</b> Cash on delivery available</span>
            <span><b aria-hidden="true">✓</b> {cart.deliveryCharge === 0 ? "Free delivery on this order" : "Delivery shown upfront"}</span>
          </div>

          <div className="cart-layout">
            <div className="cart-items-panel">
              <div className="cart-panel-heading">
                <h2>In your bag</h2>
                <span>{cart.items.length} {cart.items.length === 1 ? "product" : "products"}</span>
              </div>

              <div className="cart-item-list">
                {cart.items.map((item) => {
                  const isPending = pendingItemIds.has(item.id);
                  return (
                    <article className={`cart-item-card${isPending ? " cart-item-card--pending" : ""}`} key={item.id}>
                      <Link to={`/products/${item.productId}`} className="cart-product-image">
                        {item.imageUrls?.[0]
                          ? <img src={item.imageUrls[0]} alt={item.productName} />
                          : <span className="image-fallback">SN</span>}
                      </Link>
                      <div className="cart-product-details">
                        <p className="cart-product-category">{item.categoryName}</p>
                        <Link className="cart-product-name" to={`/products/${item.productId}`}>{item.productName}</Link>
                        <p className="cart-product-price">{currency.format(item.unitPrice)} <span>each</span></p>
                        <div className="cart-product-actions">
                          <div className="cart-quantity-control" aria-label={`Quantity for ${item.productName}`}>
                            <button
                              type="button"
                              aria-label={`Decrease ${item.productName} quantity`}
                              disabled={isPending || item.quantity <= 1}
                              onClick={() => updateQuantity(item, item.quantity - 1)}
                            >−</button>
                            <span aria-live="polite">{item.quantity}</span>
                            <button
                              type="button"
                              aria-label={`Increase ${item.productName} quantity`}
                              disabled={isPending || item.quantity >= item.availableStock}
                              onClick={() => updateQuantity(item, item.quantity + 1)}
                            >+</button>
                          </div>
                          <button
                            className="cart-remove-button"
                            type="button"
                            disabled={isPending}
                            onClick={() => removeItem(item.id)}
                          >
                            {isPending ? "Updating…" : "Remove"}
                          </button>
                        </div>
                        <p className="cart-stock-note">
                          <span aria-hidden="true">●</span>
                          {item.availableStock > 0 ? "In stock and ready to ship" : "Currently unavailable"}
                        </p>
                      </div>
                      <strong className="cart-product-total">{currency.format(item.lineTotal)}</strong>
                      {isPending && <span className="cart-item-wash" aria-hidden="true"><i /></span>}
                    </article>
                  );
                })}
              </div>

              <Link className="cart-continue-bottom" to="/products">
                <span aria-hidden="true">←</span> Continue shopping
              </Link>
            </div>

            <aside className="cart-summary cart-summary--fresh">
              <p className="eyebrow">THE FINAL DETAILS</p>
              <h2>Order summary</h2>
              <div className="summary-row"><span>Subtotal</span><strong>{currency.format(cart.subtotal)}</strong></div>
              <div className="summary-row">
                <span>Delivery</span>
                <strong>{cart.deliveryCharge === 0 ? "Free" : currency.format(cart.deliveryCharge)}</strong>
              </div>
              <div className="summary-total"><span>Total</span><strong>{currency.format(cart.total)}</strong></div>
              <p className="cart-summary-caption">Taxes and delivery are reflected in your total.</p>
              <Link className="button button--primary checkout-button cart-checkout-button" to="/checkout">
                Secure checkout <span aria-hidden="true">→</span>
              </Link>
              <div className="cart-payment-note">
                <span className="cart-shield" aria-hidden="true">✓</span>
                <p><strong>A smooth checkout, every time.</strong><span>Pay securely or choose cash on delivery.</span></p>
              </div>
            </aside>
          </div>
        </>
      )}
    </section>
  );
}
