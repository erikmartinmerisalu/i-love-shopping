import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import ReCAPTCHA from "react-google-recaptcha";
import SocialLoginButtons from "../features/auth/SocialLoginButtons";
import RecaptchaCheckbox from "../features/auth/RecaptchaCheckbox";
import { useAuth } from "../context/AuthContext";
import { isRecaptchaConfigured } from "../features/auth/oauthConfig";
import {
  type AuthFieldErrors,
  validateLoginFields,
  validateRegisterFields,
} from "../utils/authValidation";

type AuthMode = "login" | "register";

type Pending2fa = {
  email: string;
  password: string;
  oauthAccount: boolean;
};


const AuthPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { login, register, oauthLogin, verifyTwoFactorLogin, isAuthenticated } = useAuth();
  const [mode, setMode] = useState<AuthMode>(
    location.pathname.includes("register") ? "register" : "login"
  );
  const [formState, setFormState] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    twoFactorCode: "",
  });
  const [errors, setErrors] = useState<AuthFieldErrors>({});
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [successUsername, setSuccessUsername] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [requires2fa, setRequires2fa] = useState(false);
  const [pending2fa, setPending2fa] = useState<Pending2fa | null>(null);
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);
  const captchaRef = useRef<ReCAPTCHA>(null);

  const resetTwoFactorFlow = () => {
    setRequires2fa(false);
    setPending2fa(null);
    setFormState((prev) => ({ ...prev, twoFactorCode: "" }));
  };

  useEffect(() => {
    setMode(location.pathname.includes("register") ? "register" : "login");
    setRegistrationSuccess(false);
    setSuccessUsername(null);
    setErrors({});
    setCaptchaToken(null);
    captchaRef.current?.reset();
    resetTwoFactorFlow();
  }, [location.pathname]);

  const handleModeChange = (nextMode: AuthMode) => {
    setRegistrationSuccess(false);
    setSuccessUsername(null);
    navigate(nextMode === "register" ? "/register" : "/login");
  };

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setFormState((prev) => ({ ...prev, [name]: value }));
    setErrors({});
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (requires2fa && pending2fa) {
      if (formState.twoFactorCode.trim().length < 6) {
        setErrors({ twoFactorCode: "Enter the 6-digit code from your authenticator app." });
        return;
      }

      setIsSubmitting(true);
      setErrors({});

      try {
        const data = await verifyTwoFactorLogin(
          pending2fa.email,
          pending2fa.password,
          formState.twoFactorCode
        );

        if (data.success) {
          resetTwoFactorFlow();
          navigate("/products");
        } else {
          setErrors({ twoFactorCode: data.message || "Invalid authenticator code" });
        }
      } catch (error) {
        console.error("2FA verify error:", error);
        setErrors({ twoFactorCode: "Connection error. Please try again." });
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    const validationErrors =
      mode === "register"
        ? validateRegisterFields(formState.email, formState.password, formState.confirmPassword)
        : validateLoginFields(formState.email, formState.password);

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    if (mode === "register" && isRecaptchaConfigured && !captchaToken) {
      setErrors({ captcha: "Please complete the CAPTCHA check before registering." });
      return;
    }

    setIsSubmitting(true);

    try {
      let data;

      if (mode === "register") {
        data = await register(
          formState.email,
          formState.password,
          formState.confirmPassword,
          captchaToken ?? undefined
        );
      } else {
        data = await login(formState.email, formState.password);
      }

      if (data.success) {
        setRegistrationSuccess(true);
        setSuccessUsername(data.username ?? null);
        resetTwoFactorFlow();
        navigate("/products");
      } else if (data.requires2fa) {
        setPending2fa({
          email: data.email ?? formState.email,
          password: formState.password,
          oauthAccount: data.oauthAccount ?? false,
        });
        setRequires2fa(true);
        setErrors({});
      } else {
        setErrors({ email: data.message || "Authentication failed" });
        if (mode === "register" && isRecaptchaConfigured) {
          captchaRef.current?.reset();
          setCaptchaToken(null);
        }
      }
    } catch (error) {
      console.error("Auth error:", error);
      setErrors({ email: "Connection error. Please try again." });
      if (mode === "register" && isRecaptchaConfigured) {
        captchaRef.current?.reset();
        setCaptchaToken(null);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOAuthSuccess = async (token: string) => {
    setIsSubmitting(true);
    setErrors({});

    try {
      const data = await oauthLogin("google", token);
      if (data.success) {
        resetTwoFactorFlow();
        navigate("/products");
      } else if (data.requires2fa && data.email) {
        setPending2fa({
          email: data.email,
          password: "",
          oauthAccount: data.oauthAccount ?? true,
        });
        setRequires2fa(true);
        setErrors({});
      } else {
        setErrors({ email: data.message || "Google login failed" });
      }
    } catch (error) {
      console.error("OAuth error:", error);
      setErrors({ email: "OAuth login failed. Please try again." });
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated && mode === "login") {
      navigate("/products");
    }
  }, [isAuthenticated, mode, navigate]);

  const handleContinueAsGuest = () => {
    navigate("/products");
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto flex min-h-screen w-full max-w-6xl items-center px-4 py-10">
        <div className="grid w-full gap-10 rounded-[2rem] border border-white/10 bg-slate-900/80 shadow-2xl backdrop-blur-xl lg:grid-cols-[1.3fr_1fr]">
          <section className="hidden flex-col justify-between rounded-[2rem] bg-gradient-to-br from-slate-900 via-slate-950 to-slate-900 p-12 text-white lg:flex">
            <div>
              <span className="inline-flex rounded-full border border-slate-700 bg-sky-400/10 px-4 py-1 text-sm tracking-[0.18em] text-sky-300">
                💡 ESTValgus
              </span>
              <h1 className="mt-8 text-4xl font-semibold leading-tight">
                Illuminate Your Space with Custom Lighting
              </h1>
              <p className="mt-4 max-w-lg text-slate-400">
                Discover premium lighting solutions for your home. Browse our collection of smart bulbs, desk lamps, pendants, and more. Create the perfect ambiance with our curated selection.
              </p>
            </div>
            <div className="mt-10 rounded-[1.75rem] border border-white/10 bg-white/5 p-8 text-sm text-slate-300">
              <p className="mb-4 text-sm uppercase tracking-[0.22em] text-slate-400">Why choose ESTValgus?</p>
              <ul className="space-y-3">
                <li>• Premium quality lighting products</li>
                <li>• Smart home integration</li>
                <li>• Fast shipping and easy returns</li>
              </ul>
            </div>
          </section>

          <div className="flex flex-col justify-center p-8 sm:p-10">
            <div className="mb-8 lg:hidden text-center">
              <h1 className="text-3xl font-bold text-white mb-2">💡 ESTValgus</h1>
              <p className="text-slate-400 text-sm">Illuminate Your Space</p>
            </div>

            <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
              {!registrationSuccess && (
                <>
                  <div>
                    <p className="text-sm uppercase tracking-[0.24em] text-sky-300">Secure access</p>
                    <h2 className="mt-2 text-3xl font-semibold text-white">
                      {mode === "login" ? "Sign in to ESTValgus" : "Create your ESTValgus account"}
                    </h2>
                  </div>
                  <div className="rounded-3xl border border-white/10 bg-slate-950/70 px-4 py-3 text-sm text-slate-300">
                    {mode === "login" ? "No account yet?" : "Already have an account?"}{" "}
                    <button
                      type="button"
                      onClick={() => handleModeChange(mode === "login" ? "register" : "login")}
                      className="font-semibold text-white underline underline-offset-4"
                    >
                      {mode === "login" ? "Register" : "Login"}
                    </button>
                  </div>
                </>
              )}
              {registrationSuccess && (
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-sky-400/20 text-2xl border border-sky-400/30">
                    😊
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Welcome</p>
                    <p className="text-sm font-semibold text-white">{successUsername}</p>
                  </div>
                </div>
              )}
            </div>

            <div className="grid gap-4">
              {registrationSuccess ? (
                <div className="rounded-3xl border border-green-500/30 bg-green-500/10 p-6 text-center">
                  <div className="text-4xl mb-3">✅</div>
                  <h3 className="text-xl font-semibold text-green-400 mb-2">Welcome to ESTValgus!</h3>
                  <p className="text-sm text-green-300 mb-4">
                    You're now logged in. Start browsing our lighting collection.
                  </p>
                  <button
                    type="button"
                    onClick={() => navigate("/products")}
                    className="w-full rounded-3xl bg-green-500 px-4 py-3 text-sm font-semibold text-slate-950 shadow-lg shadow-green-500/10 transition hover:bg-green-400"
                  >
                    Browse Products
                  </button>
                </div>
              ) : requires2fa && pending2fa ? (
                <form onSubmit={handleSubmit} className="grid gap-4">
                  <div className="rounded-3xl border border-sky-400/30 bg-sky-400/10 p-5 space-y-3">
                    <p className="text-sm uppercase tracking-[0.2em] text-sky-300">Two-factor authentication</p>
                    <h3 className="text-xl font-semibold text-white">Enter your authenticator code</h3>
                    <p className="text-sm text-slate-300">
                      {pending2fa.oauthAccount
                        ? `Google sign-in verified for ${pending2fa.email}. Enter the 6-digit code from your authenticator app to finish logging in.`
                        : `Enter the 6-digit code from your authenticator app to finish logging in as ${pending2fa.email}.`}
                    </p>
                  </div>

                  <div>
                    <input
                      name="twoFactorCode"
                      value={formState.twoFactorCode}
                      onChange={handleChange}
                      type="text"
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      placeholder="6-digit authenticator code"
                      className={`w-full rounded-3xl border bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:ring-2 ${
                        errors.twoFactorCode
                          ? "border-red-500 focus:border-red-500 focus:ring-red-500/20"
                          : "border-slate-700 focus:border-sky-400 focus:ring-sky-400/20"
                      }`}
                    />
                    {errors.twoFactorCode && (
                      <p className="mt-1 text-xs text-red-400">{errors.twoFactorCode}</p>
                    )}
                  </div>

                  <button
                    disabled={isSubmitting || formState.twoFactorCode.trim().length < 6}
                    className="mt-2 rounded-3xl bg-sky-400 px-4 py-3 text-sm font-semibold text-slate-950 shadow-lg shadow-sky-500/10 transition hover:bg-sky-300 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {isSubmitting ? "Please wait..." : "Verify code"}
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      resetTwoFactorFlow();
                      setErrors({});
                    }}
                    className="text-sm text-slate-400 hover:text-white transition"
                  >
                    Back to login
                  </button>
                </form>
              ) : (
                <>
                  <SocialLoginButtons
                    disabled={isSubmitting}
                    onOAuthSuccess={handleOAuthSuccess}
                    onOAuthError={(message) => setErrors({ email: message })}
                  />

                  <div className="flex items-center gap-3 text-sm text-slate-400">
                    <span className="h-px flex-1 bg-slate-700"></span>
                    <span>or continue with email</span>
                    <span className="h-px flex-1 bg-slate-700"></span>
                  </div>

                  <form onSubmit={handleSubmit} className="grid gap-4">
                    <div>
                      <input
                        name="email"
                        value={formState.email}
                        onChange={handleChange}
                        type="email"
                        autoComplete="email"
                        placeholder="Email address"
                        className={`w-full rounded-3xl border bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:ring-2 ${
                          errors.email
                            ? "border-red-500 focus:border-red-500 focus:ring-red-500/20"
                            : "border-slate-700 focus:border-sky-400 focus:ring-sky-400/20"
                        }`}
                      />
                      {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email}</p>}
                    </div>

                    <div>
                      <input
                        name="password"
                        value={formState.password}
                        onChange={handleChange}
                        type="password"
                        autoComplete={mode === "register" ? "new-password" : "current-password"}
                        placeholder="Password"
                        className={`w-full rounded-3xl border bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:ring-2 ${
                          errors.password
                            ? "border-red-500 focus:border-red-500 focus:ring-red-500/20"
                            : "border-slate-700 focus:border-sky-400 focus:ring-sky-400/20"
                        }`}
                      />
                      {errors.password && <p className="mt-1 text-xs text-red-400">{errors.password}</p>}
                      {mode === "register" && !errors.password && (
                        <p className="mt-1 text-xs text-slate-500">
                          8+ characters with uppercase, lowercase, number, and special character.
                        </p>
                      )}
                    </div>

                    {mode === "register" && (
                      <div>
                        <input
                          name="confirmPassword"
                          value={formState.confirmPassword}
                          onChange={handleChange}
                          type="password"
                          autoComplete="new-password"
                          placeholder="Confirm password"
                          className={`w-full rounded-3xl border bg-slate-950/80 px-4 py-3 text-slate-100 outline-none transition focus:ring-2 ${
                            errors.confirmPassword
                              ? "border-red-500 focus:border-red-500 focus:ring-red-500/20"
                              : "border-slate-700 focus:border-sky-400 focus:ring-sky-400/20"
                          }`}
                        />
                        {errors.confirmPassword && (
                          <p className="mt-1 text-xs text-red-400">{errors.confirmPassword}</p>
                        )}
                      </div>
                    )}

                    {mode === "register" && isRecaptchaConfigured && (
                      <div>
                        <RecaptchaCheckbox
                          ref={captchaRef}
                          onChange={setCaptchaToken}
                          onExpired={() => setCaptchaToken(null)}
                        />
                        {errors.captcha && (
                          <p className="mt-1 text-xs text-red-400">{errors.captcha}</p>
                        )}
                      </div>
                    )}

                    {mode === "login" && (
                      <p className="text-right text-sm">
                        <Link to="/forgot-password" className="text-sky-300 underline">
                          Forgot password?
                        </Link>
                      </p>
                    )}

                    <button
                      disabled={isSubmitting}
                      className="mt-2 rounded-3xl bg-sky-400 px-4 py-3 text-sm font-semibold text-slate-950 shadow-lg shadow-sky-500/10 transition hover:bg-sky-300 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
                    >
                      {isSubmitting
                        ? "Please wait..."
                        : mode === "login"
                          ? "Login"
                          : "Create account"}
                    </button>
                  </form>

                  <div className="relative my-4">
                    <div className="absolute inset-0 flex items-center">
                      <div className="w-full border-t border-slate-700"></div>
                    </div>
                    <div className="relative flex justify-center text-sm">
                      <span className="px-2 bg-slate-900 text-slate-400">OR</span>
                    </div>
                  </div>

                  <button
                    onClick={handleContinueAsGuest}
                    className="w-full rounded-3xl bg-gradient-to-r from-purple-600 to-sky-400 text-white font-semibold py-3 px-4 shadow-lg shadow-purple-500/10 transition hover:shadow-xl hover:shadow-purple-500/20 flex items-center justify-center gap-2"
                  >
                    👤 Continue as Guest
                  </button>

                  <p className="text-center text-sm text-slate-500">
                    {mode === "login"
                      ? "Use your email and password for secure access."
                      : "Register once and return to this screen to sign in."}
                  </p>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
