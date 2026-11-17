import React from "react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Loading from "../components/Loading.jsx";
import ProductCard from "../components/ProductCard.jsx";
import api, { getErrorMessage } from "../services/api.js";

export default function Products() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const queryString = searchParams.toString();

  useEffect(() => {
    let active = true;
    api.get("/api/categories")
      .then((response) => { if (active) setCategories(response.data); })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    const params = new URLSearchParams(queryString);
    if (!params.has("page")) params.set("page", "0");
    if (!params.has("size")) params.set("size", "12");
    if (!params.has("sort")) params.set("sort", "newest");

    api.get("/api/products", { params })
      .then((response) => { if (active) setPageData(response.data); })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [queryString]);

  function updateParam(name, value) {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(name, value);
    else next.delete(name);
    if (name !== "page") next.delete("page");
    setSearchParams(next);
  }

  function changePage(page) {
    const next = new URLSearchParams(searchParams);
    next.set("page", String(page));
    setSearchParams(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  const selectedCategory = searchParams.get("categoryId") ?? "";
  const currentSort = searchParams.get("sort") ?? "newest";
  const products = pageData?.content ?? [];

  return (
    <section className="page-shell catalog-page">
      <div className="catalog-heading">
        <div>
          <p className="eyebrow">THE SHOP</p>
          <h1>{searchParams.get("q") ? `Results for “${searchParams.get("q")}”` : "The whole collection."}</h1>
          <p className="catalog-subtitle">A considered collection of things for the everyday.</p>
        </div>
        <Link className="text-link" to="/">← Back to home</Link>
      </div>

      <div className="catalog-toolbar">
        <label>
          <span>Category</span>
          <select value={selectedCategory} onChange={(event) => updateParam("categoryId", event.target.value)}>
            <option value="">All categories</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
        </label>
        <label>
          <span>Min price</span>
          <input
            type="number"
            min="0"
            placeholder="₹0"
            value={searchParams.get("minPrice") ?? ""}
            onChange={(event) => updateParam("minPrice", event.target.value)}
          />
        </label>
        <label>
          <span>Max price</span>
          <input
            type="number"
            min="0"
            placeholder="No limit"
            value={searchParams.get("maxPrice") ?? ""}
            onChange={(event) => updateParam("maxPrice", event.target.value)}
          />
        </label>
        <label className="sort-control">
          <span>Sort by</span>
          <select value={currentSort} onChange={(event) => updateParam("sort", event.target.value)}>
            <option value="newest">Newest</option>
            <option value="priceAsc">Price: low to high</option>
            <option value="priceDesc">Price: high to low</option>
            <option value="rating">Top rated</option>
            <option value="nameAsc">Name: A to Z</option>
          </select>
        </label>
        {searchParams.toString() && (
          <button className="clear-filters" type="button" onClick={() => setSearchParams({})}>Clear filters</button>
        )}
      </div>

      <div className="catalog-count">
        {loading ? "Finding products..." : `${pageData?.totalElements ?? 0} ${pageData?.totalElements === 1 ? "find" : "finds"}`}
      </div>

      {error ? (
        <div className="notice notice--error" role="alert">{error}</div>
      ) : loading ? (
        <Loading />
      ) : products.length ? (
        <>
          <div className="product-grid product-grid--catalog">
            {products.map((product) => <ProductCard key={product.id} product={product} />)}
          </div>
          {pageData.totalPages > 1 && (
            <nav className="pagination" aria-label="Product pages">
              <button type="button" disabled={pageData.first} onClick={() => changePage(pageData.number - 1)}>← Previous</button>
              <span>Page {pageData.number + 1} of {pageData.totalPages}</span>
              <button type="button" disabled={pageData.last} onClick={() => changePage(pageData.number + 1)}>Next →</button>
            </nav>
          )}
        </>
      ) : (
        <div className="empty-state">
          <span className="empty-icon" aria-hidden="true">⌕</span>
          <h2>No finds just yet.</h2>
          <p>Try changing your search or clearing the filters.</p>
          <button className="button button--primary" type="button" onClick={() => setSearchParams({})}>Clear filters</button>
        </div>
      )}
    </section>
  );
}
