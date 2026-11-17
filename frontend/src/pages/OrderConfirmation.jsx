import React, { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import Loading from "../components/Loading.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import api, { getErrorMessage } from "../services/api.js";

const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });

export default function OrderConfirmation() {
  const { orderId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { signOut } = useAuth();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadOrder() {
      try {
        const response = await api.get(`/api/orders/${orderId}`);
        setOrder(response.data);
      } catch (requestError) {
        if (requestError.response?.status === 401) {
          signOut();
          navigate("/login", {
            replace: true,
            state: { from: { pathname: location.pathname }, sessionExpired: true },
          });
        } else {
          setError(getErrorMessage(requestError));
        }
      } finally {
        setLoading(false);
      }
    }
    loadOrder();
  }, [location.pathname, navigate, orderId, signOut]);

  if (loading) return <section className="page-shell detail-loading"><Loading label="Loading your order..." /></section>;
  if (error) {
    return (
      <section className="page-shell checkout-page">
        <div className="notice notice--error" role="alert">{error}</div>
        <Link className="button button--primary confirmation-action" to="/products">Continue shopping</Link>
      </section>
    );
  }

  return (
    <section className="page-shell confirmation-page">
      <div className="confirmation-mark" aria-hidden="true">{order.status === "PLACED" ? "✓" : "!"}</div>
      <p className="eyebrow">ORDER {order.status === "PLACED" ? "CONFIRMED" : order.status}</p>
      <h1>{order.status === "PLACED" ? "Thank you for your order." : "Your order needs attention."}</h1>
      <p className="confirmation-copy">
        {order.status === "PLACED"
          ? <>Order <strong>{order.orderNumber}</strong> is confirmed. We’ll deliver it to the address below.</>
          : <>Order <strong>{order.orderNumber}</strong> is currently {order.status.toLowerCase()}. Check your cart or try placing the order again.</>}
      </p>

      <div className="confirmation-layout">
        <section className="checkout-card">
          <p className="eyebrow">ORDER DETAILS</p>
          {order.items.map((item, index) => (
            <div className="confirmation-item" key={`${item.productSku}-${index}`}>
              <span>{item.productName} <small>× {item.quantity}</small></span>
              <strong>{currency.format(item.lineTotal)}</strong>
            </div>
          ))}
          <div className="summary-row"><span>Subtotal</span><strong>{currency.format(order.subtotal)}</strong></div>
          <div className="summary-row"><span>Delivery</span><strong>{order.deliveryCharge === 0 ? "Free" : currency.format(order.deliveryCharge)}</strong></div>
          <div className="summary-total"><span>Total</span><strong>{currency.format(order.total)}</strong></div>
        </section>
        <section className="checkout-card confirmation-address">
          <p className="eyebrow">DELIVERING TO</p>
          <strong>{order.recipientName}</strong>
          <p>{order.addressLine1}{order.addressLine2 ? `, ${order.addressLine2}` : ""}<br />
            {order.city}, {order.state} {order.postalCode}<br />
            {order.country}<br />{order.phone}</p>
          <p className="payment-status">Payment: {order.paymentMethod === "COD" ? "Cash on delivery" : order.paymentStatus.toLowerCase()}</p>
        </section>
      </div>

      <Link className="button button--primary confirmation-action" to="/products">Continue shopping</Link>
    </section>
  );
}
