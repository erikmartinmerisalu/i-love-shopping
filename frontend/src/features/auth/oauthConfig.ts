export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? "";
export const FACEBOOK_APP_ID = import.meta.env.VITE_FACEBOOK_APP_ID ?? "";
export const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY ?? "";

export const isGoogleOAuthConfigured = GOOGLE_CLIENT_ID.length > 0;
export const isFacebookOAuthConfigured = FACEBOOK_APP_ID.length > 0;
export const isRecaptchaConfigured = RECAPTCHA_SITE_KEY.length > 0;
