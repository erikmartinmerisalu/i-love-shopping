# Frontend Implementation Guide - Security Features

## 🎯 Quick Start (What to do next)

Your backend is now fully secured with:
- ✅ Memory-only access tokens (not localStorage)
- ✅ httpOnly refresh token cookies
- ✅ CAPTCHA v3 validation on registration
- ✅ Validated OAuth tokens (Google & Facebook)

Now you need to update the frontend to use these features.

---

## Step 1: Get Google reCAPTCHA v3 Keys

1. Go to: https://www.google.com/recaptcha/admin/create
2. Fill out form:
   - **Label:** ESTValgus
   - **reCAPTCHA type:** reCAPTCHA v3
   - **Domains:** 
     - `localhost`
     - `127.0.0.1`
     - Your production domain
3. Click **Create**
4. Copy your **Site Key** (you'll need this for frontend)
5. Your **Secret Key** already in backend as `${RECAPTCHA_SECRET_KEY}`

---

## Step 2: Install reCAPTCHA Package

```bash
cd frontend
npm install @react-google-recaptcha-v3
```

---

## Step 3: Wrap App with reCAPTCHA Provider

**File: `frontend/src/main.tsx`**

```typescript
import { GoogleReCaptchaProvider } from '@react-google-recaptcha-v3';
import App from './App';
import { AuthProvider } from './context/AuthContext';

const root = ReactDOM.createRoot(document.getElementById('root')!);
root.render(
  <React.StrictMode>
    <GoogleReCaptchaProvider reCaptchaKey="YOUR_SITE_KEY_HERE">
      <AuthProvider>
        <App />
      </AuthProvider>
    </GoogleReCaptchaProvider>
  </React.StrictMode>,
);
```

Replace `YOUR_SITE_KEY_HERE` with the **Site Key** from Step 1.

---

## Step 4: Update AuthPage to Use CAPTCHA

**File: `frontend/src/pages/AuthPage.tsx`**

Add this import at top:
```typescript
import { useGoogleReCaptcha } from '@react-google-recaptcha-v3';
```

Inside the AuthPage component, add this hook:
```typescript
const { executeRecaptcha } = useGoogleReCaptcha();
```

Update the `handleSubmit` function to use CAPTCHA during registration:

```typescript
const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
  event.preventDefault();

  let isValid = false;

  if (mode === "login") {
    isValid = validateLoginFields();
  } else if (mode === "register") {
    isValid = validateRegisterFields();
  }

  if (!isValid) {
    const timer = setTimeout(() => {
      setErrors({});
    }, 2000);
    return () => clearTimeout(timer);
  }

  try {
    let data;

    if (mode === "register") {
      // Get CAPTCHA token before registration
      const captchaToken = await executeRecaptcha("register");

      // Send registration with CAPTCHA token
      data = await register(
        formState.email,
        formState.password,
        formState.confirmPassword,
        captchaToken  // ← New parameter
      );
    } else {
      data = await login(formState.email, formState.password);
    }

    if (data.success) {
      setRegistrationSuccess(true);
      setSuccessUsername(data.username ?? null);
      navigate("/products");
    } else {
      setErrors({ email: data.message || "Authentication failed" });
      setTimeout(() => setErrors({}), 2000);
    }
  } catch (error) {
    console.error("Auth error:", error);
    setErrors({ email: "Connection error. Please try again." });
    setTimeout(() => setErrors({}), 2000);
  }
};
```

---

## Step 5: Understanding the Changes

### How Tokens Work Now

**Before (Vulnerable):**
```javascript
// localStorage (accessible to XSS attacks)
localStorage.getItem('lampifyToken')  // Could be stolen

// Refresh token sent in response
{ accessToken: "...", refreshToken: "..." }
```

**After (Secure):**
```javascript
// Access token in memory only
let accessToken = "...";  // Can't access from DevTools

// Refresh token in httpOnly cookie (browser auto-sends)
// Set-Cookie: refreshToken=...; HttpOnly; SameSite=Lax

// Frontend sends Access token in header
Authorization: Bearer eyJhbGc...
```

### How CAPTCHA Works

1. **Frontend:** Before registration, get CAPTCHA token
   ```javascript
   const token = await executeRecaptcha("register");
   // Token is unique to user + action + time
   ```

2. **Backend:** Verifies token with Google
   ```java
   boolean valid = captchaValidator.validateCaptcha(token);
   // Google confirms: likely human, score > 0.5
   ```

3. **Result:** Registration proceeds or gets rejected

### How OAuth Validation Works

**Before (Vulnerable):**
```bash
Frontend claims:
{ provider: "google", email: "user@example.com" }
# Backend trusts it (anyone could claim any email!)
```

**After (Secure):**
```bash
Frontend sends:
{ provider: "google", accessToken: "xyz..." }

Backend does:
1. Verifies token signature with Google's public keys
2. Extracts email from verified token
3. Only creates user with validated email
# Forged tokens are rejected immediately
```

---

## Step 6: Environment Variables for Backend

Before running backend, set:

```bash
# Google reCAPTCHA
export RECAPTCHA_SECRET_KEY="your_secret_key_from_step_1"

# Google OAuth (optional, for now)
export GOOGLE_CLIENT_ID="your_google_client_id"
export GOOGLE_CLIENT_SECRET="your_google_client_secret"

# Facebook OAuth (optional, for now)
export FACEBOOK_APP_ID="your_facebook_app_id"
export FACEBOOK_APP_SECRET="your_facebook_app_secret"
```

Or set in `.env` file (if you're using a tool to load it).

---

## Step 7: Test Everything

### Test 1: Verify Access Token in Memory Only
```javascript
// Open DevTools, go to Console after login
console.log(localStorage.getItem('lampifyToken'));  // Should be null
console.log(sessionStorage.getItem('lampifyToken'));  // Should be null
// ✓ Token only exists in memory
```

### Test 2: Check Refresh Token Cookie
```bash
# After login, check Network tab in DevTools
# Look for Set-Cookie header in login response
# Should see: refreshToken=...; HttpOnly; SameSite=Lax

# Or in console:
document.cookie  # Won't show httpOnly cookies (that's the point!)
```

### Test 3: Test CAPTCHA (Backend)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "confirmPassword": "TestPass123!",
    "captchaChallenge": "invalid_token"
  }'

# Expected: 400 Bad Request
# Message: "CAPTCHA validation failed"
```

---

## Step 8: OAuth Setup (Later, Optional for Now)

If you want to add Google/Facebook buttons:

### Google Sign-In Button
```typescript
import { GoogleLogin } from '@react-oauth/google';

<GoogleLogin
  onSuccess={(credentialResponse) => {
    // credentialResponse.credential is the ID token
    // Send to backend: POST /api/auth/oauth/login
    // { provider: "google", accessToken: credentialResponse.credential }
  }}
/>
```

### Facebook Login Button
```typescript
import FacebookLogin from 'react-facebook-login/dist/facebook-login-render-props';

<FacebookLogin
  appId="YOUR_FACEBOOK_APP_ID"
  autoLoad={false}
  fields="name,email,picture"
  callback={(response) => {
    // Send to backend: POST /api/auth/oauth/login
    // { provider: "facebook", accessToken: response.accessToken }
  }}
/>
```

---

## Common Issues & Fixes

### Issue: "CAPTCHA validation failed" always
**Solution:**
- Check RECAPTCHA_SECRET_KEY is set correctly
- Verify Site Key matches in frontend
- CAPTCHA is disabled if `app.recaptcha.enabled=false`

### Issue: "httpOnly cookie not being set"
**Solution:**
- Check Network tab → Response Headers
- Look for `Set-Cookie` header
- May not show in localStorage (that's correct - can't access httpOnly)

### Issue: "Invalid Google/Facebook token"
**Solution:**
- Token might be expired (CAPTCHA tokens valid ~2 minutes)
- OAuth tokens must be from real provider
- Check that OAuth credentials are correct in environment variables

### Issue: "Access token lost after page reload"
**Solution:**
- This is expected! Access token is memory-only
- Frontend should call `/api/auth/refresh` on app load
- This uses the httpOnly refresh token cookie to get new access token
- AuthContext already does this automatically in useEffect

---

## Security Benefits

| Feature | Before | After | Benefit |
|---------|--------|-------|---------|
| Access Token Storage | localStorage | Memory | No XSS theft |
| Refresh Token Storage | localStorage | httpOnly cookie | No XSS/JavaScript access |
| Registration Protection | None | CAPTCHA v3 | No bot registrations |
| OAuth Validation | Trusts frontend | Validates signature | No token forgery |
| Token Format | Any | JWT with HMAC-SHA256 | Tamper-proof |
| Refresh Token Reuse | Allowed | Single-use only | No replay attacks |

---

## What Gets Called When

```
User Registration:
  1. Frontend: executeRecaptcha("register") → CAPTCHA token
  2. Frontend: POST /api/auth/register + captchaChallenge
  3. Backend: Validate CAPTCHA with Google
  4. Backend: If valid, create user, generate tokens
  5. Backend: Set-Cookie: refreshToken (httpOnly)
  6. Frontend: Stores accessToken in memory
  7. Frontend: Automatically includes Authorization header

Page Reload:
  1. Frontend: AuthContext useEffect runs
  2. Frontend: POST /api/auth/refresh (with httpOnly cookie)
  3. Backend: Validates refresh token
  4. Backend: Issues new accessToken + refreshToken cookie
  5. Frontend: Stores new accessToken in memory
  6. User stays logged in ✓

User Logout:
  1. Frontend: POST /api/auth/logout
  2. Backend: Revoke refresh token + clear cookie
  3. Frontend: Clear memory, set state to null
  4. User logged out ✓
```

---

## Next Steps

- [ ] Install `@react-google-recaptcha-v3` package
- [ ] Get reCAPTCHA Site Key from Google
- [ ] Add GoogleReCaptchaProvider to main.tsx
- [ ] Update AuthPage.tsx to use executeRecaptcha
- [ ] Set backend environment variables
- [ ] Test registration with CAPTCHA
- [ ] Test login and verify memory-only tokens
- [ ] Test page reload and refresh token
- [ ] (Later) Add Google/Facebook OAuth buttons if needed
