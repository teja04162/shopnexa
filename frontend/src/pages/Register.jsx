import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { getErrorMessage } from "../services/api.js";
import { useAuth } from "../context/AuthContext.jsx";

const initialForm = {
  fullName: "", email: "", phone: "", password: "", confirmPassword: "",
  addressLine1: "", city: "", state: "", postalCode: "", country: "",
};

export default function Register() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    setSubmitting(true);
    try {
      const response = await api.post("/api/auth/register", form);
      signIn(response.data);
      navigate("/cart");
    } catch (requestError) {
      const fieldErrors = requestError.response?.data?.fieldErrors;
      setError(fieldErrors ? Object.values(fieldErrors).join(" ") : getErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <form className="auth-card auth-card--wide" onSubmit={submit}>
        <p className="eyebrow">JOIN SHOPNEXA</p>
        <h1>Create your account.</h1>
        <p className="auth-intro">One account for your finds, wishlist, and orders.</p>
        {error && <div className="notice notice--error" role="alert">{error}</div>}
        <div className="auth-fields">
          <label>Full name<input name="fullName" autoComplete="name" required maxLength="120" value={form.fullName} onChange={update} /></label>
          <label>Email<input name="email" type="email" autoComplete="email" required value={form.email} onChange={update} /></label>
          <label>Phone<input name="phone" type="tel" autoComplete="tel" required value={form.phone} onChange={update} /></label>
          <label>Address<input name="addressLine1" autoComplete="address-line1" required value={form.addressLine1} onChange={update} /></label>
          <label>City<input name="city" autoComplete="address-level2" required value={form.city} onChange={update} /></label>
          <label>State<input name="state" autoComplete="address-level1" required value={form.state} onChange={update} /></label>
          <label>Postal code<input name="postalCode" autoComplete="postal-code" required value={form.postalCode} onChange={update} /></label>
          <label>Country<input name="country" autoComplete="country-name" required value={form.country} onChange={update} /></label>
          <label>Password<input name="password" type="password" autoComplete="new-password" minLength="8" required value={form.password} onChange={update} /></label>
          <label>Confirm password<input name="confirmPassword" type="password" autoComplete="new-password" minLength="8" required value={form.confirmPassword} onChange={update} /></label>
        </div>
        <button className="button button--primary auth-submit" type="submit" disabled={submitting}>{submitting ? "Creating account..." : "Create account"}</button>
        <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
      </form>
    </section>
  );
}
