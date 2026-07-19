import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";
import { isGoogleOAuthConfigured } from "./oauthConfig";

type GoogleSignInButtonProps = {
  onSuccess: (idToken: string) => void;
  onError: (message: string) => void;
  disabled?: boolean;
};

const GoogleSignInButton = ({ onSuccess, onError, disabled = false }: GoogleSignInButtonProps) => {
  if (!isGoogleOAuthConfigured) {
    return (
      <button
        type="button"
        disabled
        className="btn bg-white text-black border-[#e5e5e5] w-full opacity-60 cursor-not-allowed"
        title="Set VITE_GOOGLE_CLIENT_ID to enable Google login"
      >
        Login with Google (not configured)
      </button>
    );
  }

  const handleSuccess = (response: CredentialResponse) => {
    if (!response.credential) {
      onError("Google login did not return a credential");
      return;
    }
    onSuccess(response.credential);
  };

  return (
    <div className={`w-full ${disabled ? "pointer-events-none opacity-60" : ""}`}>
      <GoogleLogin
        onSuccess={handleSuccess}
        onError={() => onError("Google login failed")}
        theme="outline"
        size="large"
        text="signin_with"
        shape="rectangular"
      />
    </div>
  );
};

export default GoogleSignInButton;
