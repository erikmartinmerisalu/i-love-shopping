import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import StatusBanner from "../components/StatusBanner";
import { getPasswordError, isStrongPassword } from "../utils/authValidation";

const ResetPasswordPage = () => {
  const { resetPassword } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [resetComplete, setResetComplete] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    if (!token) {
      setError("Reset token is missing from the URL");
      return;
    }

    const passwordError = getPasswordError(password);
    if (passwordError) {
      setError(passwordError);
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    setIsSubmitting(true);
    try {
      const data = await resetPassword(token, password);
      if (data.success) {
        setResetComplete(true);
      } else {
        setError(data.message || "Unable to reset password");
      }
    } catch {
      setError("Connection error. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center px-4">
      <div className="w-full max-w-md rounded-3xl border border-white/10 bg-slate-900/80 p-8 shadow-2xl">
        {resetComplete ? (
          <div className="space-y-6 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full border border-green-500/40 bg-green-500/10 text-3xl text-green-400">
              ✓
            </div>
            <div>
              <h1 className="text-2xl font-semibold text-green-400">Password updated</h1>
              <p className="mt-2 text-sm text-green-300">
                Your password has been reset successfully. You can now sign in with your new password.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate("/login")}
              className="w-full rounded-3xl bg-green-500 px-4 py-3 font-semibold text-slate-950 transition hover:bg-green-400"
            >
              Go to login
            </button>
          </div>
        ) : (
          <>
            <h1 className="text-2xl font-semibold mb-2">Reset password</h1>
            <p className="text-sm text-slate-400 mb-6">Choose a new password for your account.</p>

            <form onSubmit={handleSubmit} className="space-y-4">
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="New password"
                className="w-full rounded-3xl border border-slate-700 bg-slate-950/80 px-4 py-3 outline-none focus:border-sky-400"
              />
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Confirm new password"
                className="w-full rounded-3xl border border-slate-700 bg-slate-950/80 px-4 py-3 outline-none focus:border-sky-400"
              />

              {!error && !isStrongPassword(password) && (
                <p className="text-xs text-slate-500">
                  8+ characters with uppercase, lowercase, number, and special character.
                </p>
              )}

              {error && <StatusBanner variant="error" title="Unable to reset password" message={error} />}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-3xl bg-sky-400 px-4 py-3 font-semibold text-slate-950 disabled:opacity-60"
              >
                {isSubmitting ? "Resetting..." : "Reset password"}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-400">
              <Link to="/login" className="text-sky-300 underline">
                Back to login
              </Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
};

export default ResetPasswordPage;
