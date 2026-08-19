import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { adminNeedsTwoFactorSetup, useAuth } from "../context/AuthContext";
import SecuritySettings from "../components/SecuritySettings";
import { formatAuthProvider, socialSignInLabel } from "../utils/authProvider";

type ProfileTab = "account" | "oauth" | "security" | "password";

const isProfileTab = (value: string | null): value is ProfileTab =>
  value === "account" || value === "oauth" || value === "security" || value === "password";

const ProfilePage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState<ProfileTab>("account");

  const isOAuthAccount = user?.oauthAccount === true;
  const providerLabel = formatAuthProvider(user?.provider);
  const showAdminGate = adminNeedsTwoFactorSetup(user);

  useEffect(() => {
    const tabParam = searchParams.get("tab");
    if (isProfileTab(tabParam)) {
      setActiveTab(tabParam);
      return;
    }
    if (searchParams.get("admin2fa") === "1") {
      setActiveTab("security");
    }
  }, [searchParams]);

  useEffect(() => {
    if (isOAuthAccount && activeTab === "password") {
      setActiveTab("account");
    }
  }, [isOAuthAccount, activeTab]);

  if (!user) {
    return null;
  }

  const tabs: { id: ProfileTab; label: string }[] = [
    { id: "account", label: "Account" },
    ...(isOAuthAccount ? [{ id: "oauth" as const, label: "OAuth sign-in" }] : []),
    { id: "security", label: "Two-factor auth" },
    ...(isOAuthAccount ? [] : [{ id: "password" as const, label: "Password" }]),
  ];

  return (
    <div className="page-container-form py-10">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Profile</h1>
          <p className="mt-1 text-sm text-slate-300">
            Signed in as {user.username}
            {isOAuthAccount && (
              <span className="ml-2 rounded-full border border-sky-500/40 bg-sky-500/10 px-2 py-0.5 text-xs font-semibold text-sky-300">
                {providerLabel} account
              </span>
            )}
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate('/products')}
          className="rounded-lg bg-gray-800 px-4 py-2 text-sm hover:bg-gray-700 transition"
        >
          Shop
        </button>
      </div>

      <div className="max-w-4xl">
        {showAdminGate && (
          <div
            className="mb-6 rounded-xl border border-amber-500/40 bg-gradient-to-br from-amber-500/15 via-gray-900 to-gray-900 p-5 sm:p-6"
            role="status"
          >
            <p className="text-xs font-semibold uppercase tracking-widest text-amber-300">Admin setup required</p>
            <h2 className="mt-2 text-lg font-bold text-white sm:text-xl">
              Enable two-factor authentication to open the admin panel
            </h2>
            <p className="mt-2 text-sm text-gray-300">
              Admin accounts must use 2FA before catalog, orders, or user management is available.
              Complete the steps below, then open Admin again from the header.
            </p>
            <ol className="mt-4 list-decimal space-y-2 pl-5 text-sm text-gray-200">
              <li>Stay on the <strong className="text-white">Two-factor auth</strong> tab (selected below).</li>
              <li>
                {isOAuthAccount
                  ? "Click Generate QR code (no password needed for OAuth accounts)."
                  : "Enter your password and click Generate QR code."}
              </li>
              <li>Scan the QR code with an authenticator app (Google Authenticator, Authy, etc.).</li>
              <li>Enter the 6-digit code and click Verify &amp; enable.</li>
              <li>
                After success, use{" "}
                <Link to="/admin" className="font-semibold text-sky-300 hover:text-sky-200">
                  Open admin panel
                </Link>{" "}
                or the Admin link in the header.
              </li>
            </ol>
          </div>
        )}

        <div className="flex flex-wrap gap-2 mb-6 border-b border-gray-800 pb-4">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-lg text-sm font-semibold transition ${
                activeTab === tab.id
                  ? "bg-primary text-white"
                  : "bg-gray-900 text-gray-300 hover:bg-gray-800"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {activeTab === "account" && (
          <div className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-4">
            <h2 className="text-xl font-bold">Account</h2>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between border-b border-gray-800 pb-3">
                <span className="text-gray-400">Username</span>
                <span>{user.username}</span>
              </div>
              <div className="flex justify-between border-b border-gray-800 pb-3">
                <span className="text-gray-400">Email</span>
                <span>{user.email}</span>
              </div>
              <div className="flex justify-between border-b border-gray-800 pb-3">
                <span className="text-gray-400">Sign-in method</span>
                <span>{isOAuthAccount ? socialSignInLabel(user.provider) : "Email & password"}</span>
              </div>
            </div>
            <p className="text-sm text-gray-400">
              {isOAuthAccount
                ? `Open the OAuth sign-in tab for details about your ${providerLabel} account.`
                : "This account uses email and password only. Google sign-in is not available for this address."}
            </p>
            {!isOAuthAccount && (
              <div className="rounded-lg border border-slate-600/40 bg-slate-800/40 p-4 text-sm text-gray-300">
                <p className="font-semibold text-white">Email &amp; password sign-in</p>
                <p className="mt-1">
                  You created this account with an ESTValgus password. Sign in on the login page with your email and
                  password, or use forgot password if needed. Google authentication is disabled for this account.
                </p>
              </div>
            )}
          </div>
        )}

        {activeTab === "oauth" && isOAuthAccount && (
          <div className="space-y-4">
            <div className="rounded-lg border border-sky-500/30 bg-gradient-to-br from-sky-500/10 via-gray-900 to-gray-900 p-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full border border-sky-400/40 bg-sky-400/10 text-xl">
                  {user.provider === "facebook" ? "f" : "G"}
                </div>
                <div className="space-y-2">
                  <p className="text-xs uppercase tracking-[0.2em] text-sky-300">OAuth sign-in</p>
                  <h2 className="text-xl font-bold">{socialSignInLabel(user.provider)}</h2>
                  <p className="text-sm text-gray-300">
                    This ESTValgus account is linked to <span className="text-white">{user.email}</span> through{" "}
                    {providerLabel}. You sign in with the {providerLabel} button on the login page — not an ESTValgus
                    password.
                  </p>
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-amber-500/30 bg-amber-500/5 p-5 space-y-3">
              <h3 className="font-semibold text-amber-200">No password on this account</h3>
              <ul className="text-sm text-gray-300 space-y-2 list-disc pl-5">
                <li>There is no ESTValgus password to reset or change here.</li>
                <li>Forgot password only applies to email &amp; password accounts.</li>
                <li>To sign in again, use <strong className="text-white">{providerLabel}</strong> on the login page.</li>
                <li>To change your {providerLabel} account password, use {providerLabel}&apos;s account settings — not ESTValgus.</li>
              </ul>
            </div>

            <button
              type="button"
              onClick={() => navigate("/login")}
              className="rounded-lg bg-gray-800 px-4 py-2 text-sm font-semibold text-white hover:bg-gray-700 transition"
            >
              Go to login
            </button>
          </div>
        )}

        {activeTab === "security" && <SecuritySettings adminGate={showAdminGate} />}

        {activeTab === "password" && !isOAuthAccount && (
          <div className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-4">
            <h2 className="text-xl font-bold">Password</h2>
            <p className="text-sm text-gray-400">
              Reset your password via email. A link will be sent to{" "}
              <span className="text-white">{user.email}</span>.
            </p>

            <Link
              to="/forgot-password"
              className="inline-flex rounded-lg bg-primary px-4 py-2 text-white hover:opacity-90 transition"
            >
              Send password reset email
            </Link>

            <p className="text-sm text-gray-500">
              Already have a reset link?{" "}
              <Link to="/reset-password" className="text-primary hover:underline">
                Enter it here
              </Link>
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfilePage;
