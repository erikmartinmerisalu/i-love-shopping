# reCAPTCHA setup (localhost + Render)

ESTValgus uses **Google reCAPTCHA v2 (“I'm not a robot” checkbox)** on the registration form. The backend verifies the token with Google's API before creating an account.

---

## Part 1 — Create keys in Google (one-time)

1. Open https://www.google.com/recaptcha/admin/create  
2. Fill in:
   - **Label:** `ESTValgus` (any name you like)
   - **reCAPTCHA type:** choose **“Challenge (v2)” → “I'm not a robot” Checkbox**
   - **Domains:** add every hostname where the shop will run (see below)
3. Submit and copy:
   - **Site key** → frontend (`VITE_RECAPTCHA_SITE_KEY`)
   - **Secret key** → backend (`RECAPTCHA_SECRET_KEY`)

### Domains to add

| Environment | Domain to add in Google admin |
|-------------|-------------------------------|
| Local Docker | `localhost` |
| Local Vite dev | `localhost` |
| Render frontend | `your-frontend-name.onrender.com` (no `https://`) |

You can add multiple domains on one reCAPTCHA site. Add Render only after you know the URL Render gives you.

---

## Part 2 — Localhost setup

### A) Docker (`http://localhost:3000`)

1. Edit the project root `.env`:

```bash
APP_RECAPTCHA_ENABLED=true
VITE_RECAPTCHA_SITE_KEY=your-site-key-here
RECAPTCHA_SECRET_KEY=your-secret-key-here
```

2. Rebuild (Vite bakes `VITE_*` at build time):

```bash
docker compose up --build
```

3. Open http://localhost:3000/register — you should see the **“I'm not a robot”** box above **Create account**.

4. Complete the checkbox, fill the form, register. If the secret key is wrong, the backend returns *“CAPTCHA validation failed”*.

### B) Vite dev server (`http://localhost:5173`)

1. Root `.env` (for backend):

```bash
APP_RECAPTCHA_ENABLED=true
RECAPTCHA_SECRET_KEY=your-secret-key-here
```

2. Frontend `.env.local`:

```bash
VITE_RECAPTCHA_SITE_KEY=your-site-key-here
```

3. Start services:

```bash
cd backend && docker compose up -d postgres
cd backend && mvn spring-boot:run
cd frontend && npm run dev
```

4. Open http://localhost:5173/register and test the checkbox.

### Troubleshooting localhost

| Problem | Fix |
|---------|-----|
| No checkbox on register | `VITE_RECAPTCHA_SITE_KEY` empty or frontend not rebuilt |
| Checkbox shows “ERROR for site owner” | Domain `localhost` missing in Google reCAPTCHA admin |
| Register fails CAPTCHA | `APP_RECAPTCHA_ENABLED=true` and matching `RECAPTCHA_SECRET_KEY` on backend |
| CAPTCHA skipped entirely | `APP_RECAPTCHA_ENABLED=false` or secret key empty |

---

## Part 3 — Render setup (for testers)

Render runs two services: **backend API** and **frontend**. CAPTCHA needs keys on **both**, plus your Render hostname in Google.

### Step 1 — Deploy and note URLs

Example (yours will differ):

| Service | Example URL |
|---------|-------------|
| Frontend | `https://estvalgus-web.onrender.com` |
| Backend | `https://estvalgus-api.onrender.com` |

### Step 2 — Update Google reCAPTCHA domains

In https://www.google.com/recaptcha/admin → your site → **Settings**:

- Add domain: `estvalgus-web.onrender.com` (hostname only, no path, no `https://`)

Save. Wait a minute for Google to propagate.

### Step 3 — Backend Web Service env vars (Render dashboard)

| Key | Value |
|-----|--------|
| `APP_RECAPTCHA_ENABLED` | `true` |
| `RECAPTCHA_SECRET_KEY` | your secret key |
| `APP_FRONTEND_URL` | `https://estvalgus-web.onrender.com` |
| `APP_CORS_ALLOWED_ORIGINS` | `https://estvalgus-web.onrender.com` |
| `APP_COOKIE_SECURE` | `true` (HTTPS on Render) |
| `SPRING_DATASOURCE_URL` | (your Postgres URL) |
| `GOOGLE_CLIENT_ID` | same value as `VITE_GOOGLE_CLIENT_ID` |

Redeploy the backend after saving env vars.

### Step 4 — Frontend env vars (Render dashboard)

Frontend must get `VITE_RECAPTCHA_SITE_KEY` **at build time**. nginx proxies `/api` to the backend using a **runtime** variable.

| Key | Value |
|-----|--------|
| `VITE_RECAPTCHA_SITE_KEY` | your site key (build-time) |
| `VITE_GOOGLE_CLIENT_ID` | same as backend, if using Google login (build-time) |
| `BACKEND_URL` | `https://estvalgus-api.onrender.com` (runtime — **no trailing slash**) |

`BACKEND_URL` tells nginx where to proxy `/api` requests. Without it, nginx looks for a host named `backend`, which only exists in Docker Compose — that causes the Render crash:

```
host not found in upstream "backend"
```

**Important:** trigger a **manual redeploy / rebuild** after changing any `VITE_*` variable. Changing only `BACKEND_URL` needs a redeploy (not a rebuild).

### Step 5 — CORS and cookies on Render

- `APP_CORS_ALLOWED_ORIGINS` must exactly match the frontend origin (`https://…onrender.com`, no trailing slash). The backend reads this at runtime — redeploy the **backend** after changing it.
- Set `APP_COOKIE_SECURE=true` on the backend when using HTTPS on Render.
- Add `https://estvalgus-web.onrender.com` to **Google Cloud → OAuth client → Authorized JavaScript origins** (required for Google login, separate from reCAPTCHA domains).

### Step 6 — Tester checklist

1. Open `https://your-frontend.onrender.com/register`
2. See reCAPTCHA checkbox
3. Tick the box, register with a strong password
4. Account is created → login works

---

## How it works in code

```
Register form → user ticks reCAPTCHA v2 → token sent as captchaChallenge
       → POST /api/auth/register
       → CaptchaValidator calls Google siteverify
       → score N/A for v2; success=true/false
       → user row created
```

- Frontend: `RecaptchaCheckbox.tsx` + register page
- Backend: `CaptchaValidator.java`, enabled when `APP_RECAPTCHA_ENABLED=true` and secret key is set
- Disabled mode: leave `APP_RECAPTCHA_ENABLED=false` or omit keys (registration works without CAPTCHA)

---

## Quick reference — env variables

| Variable | Where | Purpose |
|----------|--------|---------|
| `VITE_RECAPTCHA_SITE_KEY` | Frontend build | Shows the checkbox |
| `RECAPTCHA_SECRET_KEY` | Backend | Verifies token with Google |
| `APP_RECAPTCHA_ENABLED` | Backend | `true` = CAPTCHA required on register |

See also: [example.env](../example.env)
