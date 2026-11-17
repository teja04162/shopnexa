import React from "react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Loading from "../components/Loading.jsx";
import ProductCard from "../components/ProductCard.jsx";
import api, { getErrorMessage } from "../services/api.js";

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

export default function ProductDetails() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([
      api.get(`/api/products/${id}`),
      api.get("/api/products", { params: { page: 0, size: 4, sort: "rating" } }),
    ])
      .then(([productResponse, relatedResponse]) => {
        if (!active) return;
        setProduct(productResponse.data);
        setRelated(relatedResponse.data.content.filter((item) => item.id !== Number(id)).slice(0, 3));
      })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  if (loading) return <section className="page-shell detail-loading"><Loading label="Finding the details..." /></section>;
  if (error) {
    return (
      <section className="page-shell empty-state">
        <div className="notice notice--error" role="alert">{error}</div>
        <Link className="button button--primary" to="/products">Back to collection</Link>
      </section>
    );
  }
  if (!product) return null;

  const image = product.imageUrls?.[0];
  const displayPrice = product.discountPrice ?? product.price;
  const discount = product.discountPrice
    ? Math.round((1 - Number(product.discountPrice) / Number(product.price)) * 100)
    : null;

  return (
    <>
      <section className="page-shell product-detail">
        <nav className="breadcrumbs" aria-label="Breadcrumb">
          <Link to="/">Home</Link><span>/</span>
          <Link to={`/products?categoryId=${product.categoryId}`}>{product.categoryName}</Link><span>/</span>
          <span>{product.name}</span>
        </nav>
        <div className="detail-layout">
          <div className="detail-image">
            {image ? <img src={image} alt={product.name} /> : <span className="image-fallback">SN</span>}
            {discount && <span className="product-badge">{discount}% OFF</span>}
          </div>
          <div className="detail-copy">
            <p className="eyebrow">{product.categoryName}</p>
            <h1>{product.name}</h1>
            <div className="detail-rating"><span aria-hidden="true">★</span> {Number(product.ratingAverage).toFixed(1)} <span>({product.ratingCount} reviews)</span></div>
            <div className="detail-price">
              <strong>{currency.format(displayPrice)}</strong>
              {product.discountPrice && <del>{currency.format(product.price)}</del>}
            </div>
            <p className="detail-description">{product.description}</p>
            <div className={`stock-status ${product.stock > 0 ? "stock-status--available" : "stock-status--out"}`}>
              <span aria-hidden="true" />
              {product.stock > 0 ? (product.stock < 5 ? `Only ${product.stock} left` : "In stock") : "Currently unavailable"}
            </div>
            <div className="detail-rule" />
            <p className="detail-note">A lovely find, ready for your everyday.</p>
            <Link className="button button--primary" to={`/products?categoryId=${product.categoryId}`}>
              Explore more {product.categoryName} <span aria-hidden="true">→</span>
            </Link>
            <p className="detail-stage-note">Shopping cart and checkout are coming in the next build stage.</p>
          </div>
        </div>
      </section>

      {related.length > 0 && (
        <section className="featured-section">
          <div className="page-shell">
            <div className="section-heading">
              <div><p className="eyebrow">A FEW MORE GOOD THINGS</p><h2>You might also like</h2></div>
              <Link className="text-link" to="/products">View the collection <span aria-hidden="true">↗</span></Link>
            </div>
            <div className="product-grid">
              {related.map((item) => <ProductCard key={item.id} product={item} />)}
            </div>
          </div>
        </section>
      )}
    </>
  );
}
