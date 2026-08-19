# Part 3 — Accessibility & SEO Checklist

Supports rubric items: semantic HTML, alt text, 200% zoom, SEO best practices.

---

## Semantic HTML — why it matters (oral prep)

Screen readers and search engines rely on document structure, not visual layout.

| Bad | Good | Why |
|-----|------|-----|
| `<div onClick={...}>` | `<button type="button">` | Keyboard activatable, announced as control |
| `<div class="heading">` | `<h2>` | Heading navigation for assistive tech |
| `<span>` nav links | `<nav aria-label="Main">` | Landmark region |
| Table layout for products | `<ul>` / `<article>` grid | Logical content list |

**Project convention:** One `<h1>` per page (page title). Section titles start at `<h2>`. Product name on detail page = H1; listing card titles = H2 or H3 inside `<article>`.

---

## SEO checklist

| Item | Rule | Implementation |
|------|------|----------------|
| Title tag | ≤ 60 characters, unique per route | `react-helmet-async` in each page |
| Meta description | ~150 chars, unique | Helmet `meta name="description"` |
| URL structure | Human-readable, stable | `/products`, `/products/42`, `/search?q=` |
| Heading hierarchy | Single H1, no skipped levels | Lint in code review |
| Canonical | Avoid duplicate content | `<link rel="canonical">` on listing with filters |
| Open Graph (extra) | Product share cards | `og:title`, `og:image` on detail |

### Example titles

| Page | Title |
|------|-------|
| Home | `ESTValgus — Modern Lighting Shop` |
| Products | `Shop All Lamps — ESTValgus` |
| Product detail | `{Product Name} — ESTValgus` (truncate if > 60) |
| Admin | `Admin — Products — ESTValgus` |

---

## Image alt text

Every meaningful image needs descriptive `alt`. Decorative images use `alt=""`.

| Context | Alt pattern |
|---------|-------------|
| Product card | `{product.name} — {category}` |
| Product gallery | `{product.name}, view {n}` |
| Team photo (About) | `{Person name}, {role}` |
| Icons (cart, search) | Prefer inline SVG with `<title>` or `aria-label` on button |

**Audit command (frontend):**

```bash
rg '<img' frontend/src --glob '*.tsx' -n
# Ensure each has alt= or role=presentation
```

---

## 200% zoom readability

Browser zoom 200% must not clip primary content or require horizontal scroll on `main`.

### CSS guidelines

- Use `rem` / `%` for font sizes and spacing; avoid fixed `px` widths on containers.
- `max-width: 100%` on images and tables.
- Flex/grid with `wrap` for product grids.
- Touch targets ≥ 44×44 CSS px at 100% zoom.

### Manual test

1. Chrome → Zoom 200%.
2. Walk through Home, Product detail, Checkout, Contact.
3. Log pass/fail in `MANUAL_TEST_LOG.md`.

---

## Responsive breakpoints

| Breakpoint | Layout behavior |
|------------|-----------------|
| **320px** | Single column; hamburger nav; stacked filters |
| **768px** | 2-column product grid; sidebar filters collapsible |
| **1024px** | 3-column grid; full nav visible |
| **1440px** | Max content width ~1280px centered; 4-column grid optional |

Use mobile-first CSS:

```css
.product-grid { grid-template-columns: 1fr; }
@media (min-width: 768px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
@media (min-width: 1024px) { .product-grid { grid-template-columns: repeat(3, 1fr); } }
```

---

## Page-level semantic skeleton

```tsx
<>
  <Helmet><title>…</title></Helmet>
  <header role="banner">…</header>
  <nav aria-label="Main navigation">…</nav>
  <main id="main-content">
    <h1>Page title</h1>
    …
  </main>
  <footer role="contentinfo">…</footer>
</>
```

Skip link (extra credit):

```tsx
<a href="#main-content" className="skip-link">Skip to main content</a>
```

---

## Part 3 page sign-off

| Page | Semantic | SEO title | Alt text | 200% zoom | Responsive |
|------|----------|-----------|----------|-----------|------------|
| Home | | | | | |
| Product listing | | | | | |
| Product detail | | | | | |
| Search results | | | | | |
| Cart / quick cart | | | | | |
| Checkout | | | | | |
| Order confirmation | | | | | |
| About | | | | | |
| Contact | | | | | |
| 404 | | | | | |
| Admin | | | | | |

Check boxes during 3A/3B implementation and record in `MANUAL_TEST_LOG.md`.
