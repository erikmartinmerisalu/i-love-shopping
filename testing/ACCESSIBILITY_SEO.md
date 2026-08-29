# Accessibility & SEO

How the storefront is structured for screen readers, keyboards, and search.

## Semantic HTML

| Prefer | Avoid | Why |
|--------|--------|-----|
| `<button type="button">` | `<div onClick={...}>` | Keyboard and announced as a control |
| `<h1>`–`<h6>` in order | Styled `<div>` headings | Heading navigation |
| `<nav aria-label="Main">` | Unlabelled link lists | Landmark region |
| `<article>` / lists for products | Layout tables | Logical content |

Convention: one `<h1>` per page. Nested titles start at `<h2>`. Product name on the detail page is the H1; listing cards use a lower heading inside `<article>`.

Landmarks: `header`, `nav`, `main`, `footer`. Skip link: “Skip to main content”.

## SEO

| Item | How it is implemented |
|------|------------------------|
| Title ≤ ~60 characters, unique per route | `PageMeta` (truncates) |
| Meta description | Helmet `meta name="description"` |
| Readable URLs | `/`, `/products`, `/products/:id`, `/search?q=` |
| Canonical | Listing/filter pages |

## Images

Meaningful images get a descriptive `alt` (product name, or “Product image”). Decorative icons sit on labelled buttons (`aria-label`) rather than empty `<img>` tags.

## Layout

- Spacing and type use `rem` so 200% browser zoom still fits primary content in `main`.
- Product grids wrap; images and tables stay `max-width: 100%`.
- Breakpoints: ~320 (single column), 768 (two columns), 1024 (three columns / full nav), 1440 (capped content width).
