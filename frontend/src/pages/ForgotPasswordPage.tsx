import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import StatusBanner from "../components/StatusBanner";
import { isValidEmail } from "../utils/authValidation";

const ForgotPasswordPage = () => {
  const { forgotPassword } = useAuth();
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [emailSent, setEmailSent] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setMessage("");
    setEmailSent(false);

    if (!isValidEmail(email)) {
      setError("Enter a valid email address");
      return;
    }

    setIsSubmitting(true);
    try {
      const data = await forgotPassword(email);
      if (data.success) {
        setMessage(data.message);
        setEmailSent(true);
      } else {
        setError(data.message || "Unable to process request");
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
        <h1 className="text-2xl font-semibold mb-2">Forgot password</h1>
        <p className="text-sm text-slate-400 mb-4">
          For accounts created with email and password only. We will send a reset link if that type of account exists.
        </p>

        <StatusBanner
          variant="info"
          title="Signed up with Google?"
          message="Google accounts do not use an ESTValgus password. Go back to login and use the Google button instead — forgot password will not work for those accounts."
        />

        <div className="mt-6">
          {emailSent ? (
            <div className="space-y-6">
              <StatusBanner
                variant="success"
                title="Check your email"
                message={message || "If the account exists, a reset link has been sent."}
              />
              <p className="text-sm text-slate-400">
                Didn't receive it? Check spam, or ask your admin for the reset link in local dev.
              </p>
              <Link
                to="/login"
                className="block w-full rounded-3xl bg-sky-400 px-4 py-3 text-center font-semibold text-slate-950"
              >
                Back to login
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email address"
                className="w-full rounded-3xl border border-slate-700 bg-slate-950/80 px-4 py-3 outline-none focus:border-sky-400"
              />

              {error && <StatusBanner variant="error" title="Unable to send reset link" message={error} />}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-3xl bg-sky-400 px-4 py-3 font-semibold text-slate-950 disabled:opacity-60"
              >
                {isSubmitting ? "Sending..." : "Send reset link"}
              </button>
            </form>
          )}

          {!emailSent && (
            <p className="mt-6 text-center text-sm text-slate-400">
              <Link to="/login" className="text-sky-300 underline">
                Back to login
              </Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
