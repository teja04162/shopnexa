import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import ProductCard from "../components/ProductCard.jsx";
import Loading from "../components/Loading.jsx";
import api, { getErrorMessage } from "../services/api.js";

export default function Wishlist() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      const response = await api.get("/api/wishlist");
      setProducts(response.data.items);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  async function remove(productId) {
    try {
      const response = await api.delete(`/api/wishlist/${productId}`);
      setProducts(response.data.items);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  return (
    <section className="page-shell catalog-page">
      <p className="eyebrow">SAVED FOR LATER</p>
      <h1 className="page-title">Your wishlist</h1>
      {error && <div className="notice notice--error" role="alert">{error}</div>}
      {loading ? <Loading label="Loading your wishlist..." /> : products.length ? (
        <div className="product-grid product-grid--catalog">
          {products.map((product) => (
            <div className="wishlist-card" key={product.id}>
              <ProductCard product={product} />
              <button className="wishlist-remove" type="button" onClick={() => remove(product.id)}>Remove from wishlist</button>
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <span className="empty-icon" aria-hidden="true">♡</span>
          <h2>Keep the good ones close.</h2>
          <p>Save products you love and they’ll be waiting here.</p>
          <Link className="button button--primary" to="/products">Explore products</Link>
        </div>
      )}
    </section>
  );
}
