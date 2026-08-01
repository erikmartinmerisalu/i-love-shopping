import { forwardRef } from "react";
import ReCAPTCHA from "react-google-recaptcha";
import { RECAPTCHA_SITE_KEY } from "./oauthConfig";

type RecaptchaCheckboxProps = {
  onChange: (token: string | null) => void;
  onExpired?: () => void;
};

const RecaptchaCheckbox = forwardRef<ReCAPTCHA, RecaptchaCheckboxProps>(
  ({ onChange, onExpired }, ref) => {
    if (!RECAPTCHA_SITE_KEY) {
      return null;
    }

    return (
      <div className="rounded-2xl border border-slate-700 bg-slate-950/60 p-3">
        <p className="mb-3 text-xs text-slate-400">
          Complete the check below to prove you are not a bot.
        </p>
        <ReCAPTCHA ref={ref} sitekey={RECAPTCHA_SITE_KEY} onChange={onChange} onExpired={onExpired} />
      </div>
    );
  }
);

RecaptchaCheckbox.displayName = "RecaptchaCheckbox";

export default RecaptchaCheckbox;
