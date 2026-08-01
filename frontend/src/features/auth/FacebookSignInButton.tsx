import { useEffect, useState } from "react";
import { FACEBOOK_APP_ID, isFacebookOAuthConfigured } from "./oauthConfig";

type FacebookLoginResponse = {
  authResponse?: {
    accessToken: string;
  };
  status: string;
};

type FacebookSignInButtonProps = {
  onSuccess: (accessToken: string) => void;
  onError: (message: string) => void;
  disabled?: boolean;
};

declare global {
  interface Window {
    FB?: {
      init: (params: { appId: string; cookie: boolean; xfbml: boolean; version: string }) => void;
      login: (callback: (response: FacebookLoginResponse) => void, options: { scope: string }) => void;
    };
    fbAsyncInit?: () => void;
  }
}

const FacebookSignInButton = ({ onSuccess, onError, disabled = false }: FacebookSignInButtonProps) => {
  const [sdkReady, setSdkReady] = useState(false);

  useEffect(() => {
    if (!isFacebookOAuthConfigured) {
      return;
    }

    if (window.FB) {
      setSdkReady(true);
      return;
    }

    window.fbAsyncInit = () => {
      window.FB?.init({
        appId: FACEBOOK_APP_ID,
        cookie: true,
        xfbml: false,
        version: "v19.0",
      });
      setSdkReady(true);
    };

    const existingScript = document.getElementById("facebook-jssdk");
    if (!existingScript) {
      const script = document.createElement("script");
      script.id = "facebook-jssdk";
      script.async = true;
      script.defer = true;
      script.src = "https://connect.facebook.net/en_US/sdk.js";
      document.body.appendChild(script);
    }
  }, []);

  const handleClick = () => {
    if (!window.FB) {
      onError("Facebook SDK is not ready yet");
      return;
    }

    window.FB.login(
      (response) => {
        if (response.authResponse?.accessToken) {
          onSuccess(response.authResponse.accessToken);
          return;
        }

        if (response.status === "unknown") {
          onError("Facebook login was cancelled");
          return;
        }

        onError("Facebook login failed");
      },
      { scope: "email,public_profile" }
    );
  };

  if (!isFacebookOAuthConfigured) {
    return (
      <button
        type="button"
        disabled
        className="btn bg-[#1A77F2] text-white border-[#005fd8] w-full opacity-60 cursor-not-allowed"
        title="Set VITE_FACEBOOK_APP_ID to enable Facebook login"
      >
        Login with Facebook (not configured)
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={disabled || !sdkReady}
      className="btn bg-[#1A77F2] text-white border-[#005fd8] hover:bg-[#1666d1] w-full disabled:opacity-60 disabled:cursor-not-allowed"
    >
      <svg width="16" height="16" viewBox="0 0 32 32" aria-hidden="true">
        <path fill="white" d="M8 12h5V8c0-6 4-7 11-6v5c-4 0-5 0-5 3v2h5l-1 6h-4v12h-6V18H8z"></path>
      </svg>
      {sdkReady ? "Login with Facebook" : "Loading Facebook..."}
    </button>
  );
};

export default FacebookSignInButton;
