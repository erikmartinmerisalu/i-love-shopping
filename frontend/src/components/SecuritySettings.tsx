import { useState, type FormEvent } from "react";
import { useAuth } from "../context/AuthContext";
import { socialSignInLabel } from "../utils/authProvider";
import StatusBanner from "./StatusBanner";

type TwoFactorStatus = "idle" | "enabled" | "disabled";

const SecuritySettings = () => {
  const { user, setupTwoFactor, verifyTwoFactorSetup, disableTwoFactor } = useAuth();
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [qrCodeUri, setQrCodeUri] = useState<string | null>(null);
  const [backupCodes, setBackupCodes] = useState<string[]>([]);
  const [infoMessage, setInfoMessage] = useState("");
  const [error, setError] = useState("");
  const [twoFactorStatus, setTwoFactorStatus] = useState<TwoFactorStatus>("idle");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!user) {
    return null;
  }

  const isOAuthAccount = user.oauthAccount === true;

  const clearFeedback = () => {
    setError("");
    setInfoMessage("");
    setTwoFactorStatus("idle");
  };

  const handleSetup = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    clearFeedback();
    setIsSubmitting(true);

    try {
      const data = await setupTwoFactor(user.email, isOAuthAccount ? "" : password);
      if (data.success) {
        setQrCodeUri(data.qrCodeUri ?? null);
        setBackupCodes(data.backupCodes ?? []);
        setInfoMessage(data.message || "Scan the QR code with your authenticator app.");
      } else {
        setError(data.message || "Unable to start 2FA setup");
      }
    } catch {
      setError("Connection error. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifySetup = async () => {
    clearFeedback();
    setIsSubmitting(true);

    try {
      const data = await verifyTwoFactorSetup(user.email, isOAuthAccount ? "" : password, code);
      if (data.success) {
        setTwoFactorStatus("enabled");
        setQrCodeUri(null);
        setBackupCodes([]);
        setCode("");
        setPassword("");
      } else {
        setError(data.message || "Invalid verification code");
      }
    } catch {
      setError("Connection error. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDisable = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    clearFeedback();
    setIsSubmitting(true);

    try {
      const data = await disableTwoFactor(user.email, isOAuthAccount ? "" : password, code);
      if (data.success) {
        setTwoFactorStatus("disabled");
        setQrCodeUri(null);
        setBackupCodes([]);
        setCode("");
        setPassword("");
      } else {
        setError(data.message || "Unable to disable 2FA");
      }
    } catch {
      setError("Connection error. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-6">
      <div>
        <h2 className="text-xl font-bold">Two-factor authentication</h2>
        <p className="text-sm text-gray-400 mt-1">
          {isOAuthAccount
            ? `${socialSignInLabel(user.provider)} — no ESTValgus password is required to manage 2FA.`
            : `Confirm your ESTValgus password to manage 2FA for ${user.email}.`}
        </p>
      </div>

      {twoFactorStatus === "enabled" && (
        <StatusBanner
          variant="success"
          title="Two-factor authentication enabled"
          message="Your account is now protected. You'll need your authenticator app each time you sign in."
        />
      )}

      {twoFactorStatus === "disabled" && (
        <StatusBanner
          variant="info"
          title="Two-factor authentication disabled"
          message="You can re-enable 2FA at any time using the form below."
        />
      )}

      {error && (
        <StatusBanner variant="error" title="Something went wrong" message={error} />
      )}

      {infoMessage && twoFactorStatus === "idle" && !qrCodeUri && (
        <StatusBanner variant="info" title="Next step" message={infoMessage} />
      )}

      <form onSubmit={handleSetup} className="space-y-3">
        <h3 className="font-semibold">Enable 2FA</h3>
        {!isOAuthAccount && (
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Confirm your password"
            className="w-full rounded-lg border border-gray-700 bg-gray-950 px-4 py-2"
          />
        )}
        <button
          type="submit"
          disabled={isSubmitting || (!isOAuthAccount && !password)}
          className="rounded-lg bg-primary px-4 py-2 text-white disabled:opacity-60"
        >
          Generate QR code
        </button>
      </form>

      {qrCodeUri && (
        <div className="space-y-3 rounded-lg border border-sky-500/30 bg-sky-500/5 p-4">
          <StatusBanner
            variant="info"
            title="Scan your QR code"
            message="Open your authenticator app, add a new account, and scan the code below."
          />
          <img src={qrCodeUri} alt="2FA QR code" className="w-48 h-48 bg-white p-2 rounded" />
          {backupCodes.length > 0 && (
            <div className="rounded-lg border border-gray-700 bg-gray-950 p-3">
              <p className="text-sm font-semibold text-gray-200 mb-2">Backup codes (store securely)</p>
              <div className="grid grid-cols-2 gap-2 text-sm font-mono text-sky-300">
                {backupCodes.map((backupCode) => (
                  <span key={backupCode}>{backupCode}</span>
                ))}
              </div>
            </div>
          )}
          <div className="flex gap-2">
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="6-digit code"
              className="flex-1 rounded-lg border border-gray-700 bg-gray-950 px-4 py-2"
            />
            <button
              type="button"
              onClick={handleVerifySetup}
              disabled={isSubmitting || !code}
              className="rounded-lg bg-green-600 px-4 py-2 text-white disabled:opacity-60"
            >
              Verify & enable
            </button>
          </div>
        </div>
      )}

      <form onSubmit={handleDisable} className="space-y-3 border-t border-gray-800 pt-4">
        <h3 className="font-semibold">Disable 2FA</h3>
        {!isOAuthAccount && (
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Confirm your password"
            className="w-full rounded-lg border border-gray-700 bg-gray-950 px-4 py-2"
          />
        )}
        <input
          type="text"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="Authenticator or backup code"
          className="w-full rounded-lg border border-gray-700 bg-gray-950 px-4 py-2"
        />
        <button
          type="submit"
          disabled={isSubmitting || !code || (!isOAuthAccount && !password)}
          className="rounded-lg bg-red-600 px-4 py-2 text-white disabled:opacity-60"
        >
          Disable 2FA
        </button>
      </form>
    </div>
  );
};

export default SecuritySettings;
