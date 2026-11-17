import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Loading from "../components/Loading.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import api, { getErrorMessage } from "../services/api.js";

const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });

const emptyAddress = {
  recipientName: "",
  phone: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "India",
};

function loadRazorpaySdk() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve();
      return;
    }

    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Razorpay checkout could not be loaded."));
    document.body.appendChild(script);
  });
}

export default function Checkout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [paymentOptions, setPaymentOptions] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState("COD");
  const [form, setForm] = useState(emptyAddress);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  function redirectForExpiredSession(requestError, pathname = "/checkout") {
    if (requestError.response?.status !== 401) return false;
    signOut();
    navigate("/login", {
      replace: true,
      state: { from: { pathname }, sessionExpired: true },
    });
    return true;
  }

  useEffect(() => {
    setForm((current) => ({
      ...current,
      recipientName: current.recipientName || user?.fullName || "",
      phone: current.phone || user?.phone || "",
    }));
  }, [user]);

  useEffect(() => {
    async function loadCheckout() {
      try {
        const [cartResponse, optionsResponse] = await Promise.all([
          api.get("/api/cart"),
          api.get("/api/checkout/options"),
        ]);
        setCart(cartResponse.data);
        setPaymentOptions(optionsResponse.data);
      } catch (requestError) {
        if (!redirectForExpiredSession(requestError)) setError(getErrorMessage(requestError));
      } finally {
        setLoading(false);
      }
    }
    loadCheckout();
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function cancelPendingPayment(orderId, message) {
    try {
      await api.post(`/api/orders/${orderId}/cancel-payment`);
      setError(message);
    } catch (requestError) {
      if (!redirectForExpiredSession(requestError)) {
        setError(`${message} ${getErrorMessage(requestError)}`);
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function placeOrder(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      const { data: checkout } = await api.post("/api/orders", { ...form, paymentMethod });
      if (!checkout.paymentRequired) {
        navigate(`/orders/${checkout.orderId}/confirmation`);
        return;
      }

      try {
        await loadRazorpaySdk();
      } catch {
        await cancelPendingPayment(
          checkout.orderId,
          "The payment window could not be opened. Your items have been returned to the cart."
        );
        return;
      }

      let paymentFinished = false;
      let cancellationStarted = false;
      const cancelPayment = () => {
        if (paymentFinished || cancellationStarted) return;
        cancellationStarted = true;
        void cancelPendingPayment(
          checkout.orderId,
          "Payment was cancelled. Your items have been returned to the cart."
        );
      };

      try {
        const payment = new window.Razorpay({
          key: checkout.razorpayKeyId,
          amount: checkout.amountInPaise,
          currency: checkout.currency,
          name: "ShopNexa",
          description: `Order ${checkout.orderNumber}`,
          order_id: checkout.razorpayOrderId,
          handler: async (result) => {
            paymentFinished = true;
            try {
              await api.post(`/api/orders/${checkout.orderId}/verify-payment`, {
                razorpayPaymentId: result.razorpay_payment_id,
                razorpayOrderId: result.razorpay_order_id,
                razorpaySignature: result.razorpay_signature,
              });
              navigate(`/orders/${checkout.orderId}/confirmation`);
            } catch (requestError) {
              if (!redirectForExpiredSession(requestError)) setError(getErrorMessage(requestError));
              setSubmitting(false);
            }
          },
          modal: { ondismiss: cancelPayment },
        });

        payment.on("payment.failed", cancelPayment);
        payment.open();
      } catch {
        await cancelPendingPayment(
          checkout.orderId,
          "The payment window could not be opened. Your items have been returned to the cart."
        );
      }
    } catch (requestError) {
      if (!redirectForExpiredSession(requestError)) setError(getErrorMessage(requestError));
      setSubmitting(false);
    }
  }

  if (loading) {
    return <section className="page-shell detail-loading"><Loading label="Preparing checkout..." /></section>;
  }

  if (error && !cart) {
    return (
      <section className="page-shell checkout-page">
        <p className="eyebrow">CHECKOUT</p>
        <div className="notice notice--error" role="alert">{error}</div>
        <Link className="button button--primary confirmation-action" to="/cart">Return to cart</Link>
      </section>
    );
  }

  if (!cart?.items?.length) {
    return (
      <section className="page-shell checkout-page">
        <p className="eyebrow">CHECKOUT</p>
        <div className="empty-state">
          <h1>Your cart is empty.</h1>
          <p>Add something to your bag before checking out.</p>
          <Link className="button button--primary" to="/products">Browse the collection</Link>
        </div>
      </section>
    );
  }

  return (
    <section className="page-shell checkout-page">
      <p className="eyebrow">CHECKOUT</p>
      <h1 className="page-title">Delivery &amp; payment</h1>
      <Link className="text-link checkout-back" to="/cart">← Back to cart</Link>
      {error && <div className="notice notice--error" role="alert">{error}</div>}

      <div className="checkout-layout">
        <form className="checkout-form" onSubmit={placeOrder}>
          <section className="checkout-card">
            <p className="eyebrow">01 — DELIVERY</p>
            <h2>Where should we send it?</h2>
            <div className="checkout-fields">
              <label>
                Full name
                <input name="recipientName" autoComplete="name" required maxLength="120" value={form.recipientName} onChange={updateField} />
              </label>
              <label>
                Phone
                <input name="phone" autoComplete="tel" required maxLength="30" value={form.phone} onChange={updateField} />
              </label>
              <label className="checkout-field--wide">
                Address line 1
                <input name="addressLine1" autoComplete="address-line1" required maxLength="200" value={form.addressLine1} onChange={updateField} />
              </label>
              <label className="checkout-field--wide">
                Address line 2 <span className="field-optional">(optional)</span>
                <input name="addressLine2" autoComplete="address-line2" maxLength="200" value={form.addressLine2} onChange={updateField} />
              </label>
              <label>
                City
                <input name="city" autoComplete="address-level2" required maxLength="100" value={form.city} onChange={updateField} />
              </label>
              <label>
                State
                <input name="state" autoComplete="address-level1" required maxLength="100" value={form.state} onChange={updateField} />
              </label>
              <label>
                Postal code
                <input name="postalCode" autoComplete="postal-code" required maxLength="20" value={form.postalCode} onChange={updateField} />
              </label>
              <label>
                Country
                <input name="country" autoComplete="country-name" required maxLength="100" value={form.country} onChange={updateField} />
              </label>
            </div>
          </section>

          <section className="checkout-card">
            <p className="eyebrow">02 — PAYMENT</p>
            <h2>Choose how to pay</h2>
            <label className={`payment-option${paymentMethod === "COD" ? " payment-option--selected" : ""}`}>
              <input
                type="radio"
                name="paymentMethod"
                value="COD"
                checked={paymentMethod === "COD"}
                onChange={() => setPaymentMethod("COD")}
              />
              <span><strong>Cash on delivery</strong><small>Pay when your order arrives.</small></span>
            </label>
            {paymentOptions?.razorpayEnabled && (
              <label className={`payment-option${paymentMethod === "RAZORPAY" ? " payment-option--selected" : ""}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="RAZORPAY"
                  checked={paymentMethod === "RAZORPAY"}
                  onChange={() => setPaymentMethod("RAZORPAY")}
                />
                <span><strong>Pay online</strong><small>Secure test-mode checkout powered by Razorpay.</small></span>
              </label>
            )}
            {!paymentOptions?.razorpayEnabled && (
              <p className="payment-unavailable">
                Online payment is not set up yet. Cash on delivery is available; add Razorpay test keys to enable online payment.
              </p>
            )}
            <button className="button button--primary checkout-button" type="submit" disabled={submitting}>
              {submitting ? "Processing..." : paymentMethod === "COD" ? "Place order" : "Continue to payment"}
            </button>
          </section>
        </form>

        <aside className="cart-summary checkout-summary">
          <p className="eyebrow">YOUR ORDER</p>
          <h2>Order summary</h2>
          <div className="checkout-order-items">
            {cart.items.map((item) => (
              <div className="checkout-order-item" key={item.id}>
                <span>{item.productName} <small>× {item.quantity}</small></span>
                <strong>{currency.format(item.lineTotal)}</strong>
              </div>
            ))}
          </div>
          <div className="summary-row"><span>Subtotal</span><strong>{currency.format(cart.subtotal)}</strong></div>
          <div className="summary-row"><span>Delivery</span><strong>{cart.deliveryCharge === 0 ? "Free" : currency.format(cart.deliveryCharge)}</strong></div>
          <div className="summary-total"><span>Total</span><strong>{currency.format(cart.total)}</strong></div>
          <p className="summary-note">Final prices and stock are confirmed securely by ShopNexa when you place the order.</p>
        </aside>
      </div>
    </section>
  );
}
