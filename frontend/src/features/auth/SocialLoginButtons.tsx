import GoogleSignInButton from "./GoogleSignInButton";

interface SocialLoginButtonsProps {
  onOAuthSuccess: (token: string) => void;
  onOAuthError: (message: string) => void;
  disabled?: boolean;
}

const SocialLoginButtons = ({ onOAuthSuccess, onOAuthError, disabled = false }: SocialLoginButtonsProps) => {
  return (
    <GoogleSignInButton
      disabled={disabled}
      onSuccess={onOAuthSuccess}
      onError={onOAuthError}
    />
  );
};

export default SocialLoginButtons;
