import React from "react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { getErrorMessage } from "../services/api.js";
import Loading from "../components/Loading.jsx";
import ProductCard from "../components/ProductCard.jsx";

const currency = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

export default function Home() {
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([
      api.get("/api/categories"),
      api.get("/api/products", { params: { page: 0, size: 20, sort: "newest" } }),
    ])
      .then(([categoryResponse, productResponse]) => {
        if (!active) return;
        setCategories(categoryResponse.data);
        setProducts(productResponse.data.content);
      })
      .catch((requestError) => {
        if (active) setError(getErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, []);

  const heroProduct = products[0];
  const heroImage = heroProduct?.imageUrls?.[0];
  const supportingProducts = products.slice(1, 3);
  const popularProducts = [...products]
    .sort((first, second) => Number(second.ratingAverage) - Number(first.ratingAverage))
    .slice(0, 8);
  const popularProductIds = new Set(popularProducts.map((product) => product.id));
  const moreProducts = products.filter((product) => !popularProductIds.has(product.id));

  return (
    <div className="shop-home">
      <section className="shop-hero">
        <div className="shop-hero-copy">
          <p className="shop-kicker">WELCOME TO SHOPNEXA</p>
          <h1>Everything you need,<br /><span>all in one place.</span></h1>
          <p className="shop-hero-description">Discover great finds across electronics, fashion, home, books, and more.</p>
          <div className="shop-hero-actions">
            <Link className="shop-button" to="/products">
              Shop all products <span aria-hidden="true">→</span>
            </Link>
            <Link className="shop-secondary-link" to="/#shop-categories">
              Browse categories
            </Link>
          </div>
          <p className="shop-hero-note">Free delivery on orders above ₹1,000</p>
        </div>

        <div className="shop-hero-products">
          {heroProduct ? (
            <Link className="shop-hero-product shop-hero-product--main" to={`/products/${heroProduct.id}`}>
              {heroImage && <img src={heroImage} alt={heroProduct.name} />}
              <span className="shop-hero-product-info">
                <span>Featured product</span>
                <strong>{heroProduct.name}</strong>
                <b>{currency.format(heroProduct.discountPrice ?? heroProduct.price)}</b>
              </span>
            </Link>
          ) : (
            <div className="shop-hero-product shop-hero-product--main shop-hero-placeholder">ShopNexa</div>
          )}
          {supportingProducts.map((product) => {
            const image = product.imageUrls?.[0];
            return (
              <Link className="shop-hero-product shop-hero-product--small" key={product.id} to={`/products/${product.id}`}>
                {image && <img src={image} alt={product.name} />}
                <span>{product.name}</span>
              </Link>
            );
          })}
        </div>
      </section>

      <section className="shop-section shop-products">
        <div className="shop-section-inner">
          <div className="shop-section-heading">
            <div>
              <p className="shop-eyebrow">CUSTOMER FAVORITES</p>
              <h2>Popular products</h2>
            </div>
            <Link className="shop-view-all" to="/products">View all products <span aria-hidden="true">→</span></Link>
          </div>
          {error ? (
            <div className="notice notice--error" role="alert">{error}</div>
          ) : loading ? (
            <Loading label="Loading popular products..." />
          ) : popularProducts.length ? (
            <div className="product-grid shop-product-grid">
              {popularProducts.map((product) => <ProductCard key={product.id} product={product} />)}
            </div>
          ) : (
            <p className="empty-copy">Products will appear here when the catalog is ready.</p>
          )}
        </div>
      </section>

      <section className="shop-section shop-categories" id="shop-categories">
        <div className="shop-section-inner">
          <div className="shop-section-heading">
          <div>
              <p className="shop-eyebrow">SHOP BY DEPARTMENT</p>
              <h2>Explore categories</h2>
          </div>
            <Link className="shop-view-all" to="/products">Shop all <span aria-hidden="true">→</span></Link>
          </div>

          {error ? (
            <div className="notice notice--error" role="alert">{error}</div>
          ) : loading ? (
            <Loading label="Loading categories..." />
          ) : categories.length ? (
            <div className="shop-category-grid">
              {categories.map((category, index) => {
                const representative = products.find((product) => product.categoryId === category.id && product.id !== heroProduct?.id)
                  ?? products.find((product) => product.categoryId === category.id);
                const image = representative?.imageUrls?.[0];
                return (
                  <Link className="shop-category-card" key={category.id} to={`/products?categoryId=${category.id}`}>
                    <span className="shop-category-image">
                      {image ? <img src={image} alt="" loading="lazy" /> : <span aria-hidden="true">SN</span>}
                    </span>
                    <span className="shop-category-name">{category.name}</span>
                  </Link>
                );
              })}
            </div>
          ) : (
            <p className="empty-copy">Categories will appear here when the catalog is ready.</p>
          )}
        </div>
      </section>

      {moreProducts.length > 0 && (
        <section className="shop-section shop-more-products">
          <div className="shop-section-inner">
            <div className="shop-section-heading">
              <div>
                <p className="shop-eyebrow">MORE TO DISCOVER</p>
                <h2>More products</h2>
              </div>
              <Link className="shop-view-all" to="/products">Browse all products <span aria-hidden="true">→</span></Link>
            </div>
            {error ? (
              <div className="notice notice--error" role="alert">{error}</div>
            ) : (
              <div className="product-grid shop-product-grid">
                {moreProducts.map((product) => <ProductCard key={product.id} product={product} />)}
              </div>
            )}
          </div>
        </section>
      )}

      <div className="shop-home-footer-link">
        <Link className="shop-button" to="/products">Browse the full catalog <span aria-hidden="true">→</span></Link>
      </div>
    </div>
  );
}
