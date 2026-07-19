import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import SecuritySettings from "../components/SecuritySettings";
import StatusBanner from "../components/StatusBanner";

type ProfileTab = "account" | "security" | "password";

const ProfilePage = () => {
  const { user, logout, forgotPassword } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<ProfileTab>("account");
  const [passwordMessage, setPasswordMessage] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [isSendingReset, setIsSendingReset] = useState(false);

  const isOAuthAccount = user?.oauthAccount === true;

  useEffect(() => {
    if (isOAuthAccount && activeTab === "password") {
      setActiveTab("account");
    }
  }, [isOAuthAccount, activeTab]);

  if (!user) {
    return null;
  }

  const handlePasswordReset = async () => {
    setPasswordError("");
    setPasswordMessage("");
    setIsSendingReset(true);

    try {
      const data = await forgotPassword(user.email);
      if (data.success) {
        setPasswordMessage(data.message || "If the account exists, a reset link has been sent.");
      } else {
        setPasswordError(data.message || "Unable to send reset email.");
      }
    } catch {
      setPasswordError("Connection error. Please try again.");
    } finally {
      setIsSendingReset(false);
    }
  };

  const tabs: { id: ProfileTab; label: string }[] = [
    { id: "account", label: "Account" },
    { id: "security", label: "Two-factor auth" },
    ...(isOAuthAccount ? [] : [{ id: "password" as const, label: "Password" }]),
  ];

  return (
    <div className="min-h-screen bg-gray-950 text-white">
      <header className="bg-gray-900 border-b border-gray-800 sticky top-0 z-40">
        <div className="flex justify-between items-center px-4 sm:px-6 lg:px-10 xl:px-16 py-4 w-full">
          <div>
            <h1 className="text-3xl lg:text-4xl font-bold text-primary">ESTValgus</h1>
            <div className="mt-1 flex flex-wrap items-center gap-2 sm:gap-3">
              <p className="text-sm text-slate-300">Signed in as {user.username}</p>
              <span className="text-xs text-slate-500">· Account settings</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate("/products")}
              className="bg-gray-800 text-white px-4 py-2 rounded-lg hover:bg-gray-700 transition"
            >
              Shop
            </button>
            <button
              onClick={logout}
              className="bg-rose-700 text-white px-4 py-2 rounded-lg hover:bg-rose-600 transition font-semibold border border-rose-500/40 shadow-sm shadow-rose-900/30"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-3xl mx-auto py-8 px-4 sm:px-6">
        <div className="flex gap-2 mb-6 border-b border-gray-800 pb-4">
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
                <span className="capitalize">{user.oauthAccount ? user.provider ?? "Google" : "Email & password"}</span>
              </div>
            </div>
            <p className="text-sm text-gray-400">
              {isOAuthAccount
                ? "Manage two-factor authentication in the Security tab."
                : "Manage security and password options in the other tabs."}
            </p>
          </div>
        )}

        {activeTab === "security" && <SecuritySettings />}

        {activeTab === "password" && !isOAuthAccount && (
          <div className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-4">
            <h2 className="text-xl font-bold">Password</h2>
            <p className="text-sm text-gray-400">
              Reset your password via email. A link will be sent to{" "}
              <span className="text-white">{user.email}</span> if the account exists.
            </p>

            {passwordError && (
              <StatusBanner variant="error" title="Unable to send reset email" message={passwordError} />
            )}
            {passwordMessage && (
              <StatusBanner
                variant="success"
                title="Reset email sent"
                message={passwordMessage}
              />
            )}

            <button
              type="button"
              onClick={handlePasswordReset}
              disabled={isSendingReset}
              className="rounded-lg bg-primary px-4 py-2 text-white disabled:opacity-60"
            >
              {isSendingReset ? "Sending..." : "Send password reset email"}
            </button>

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
