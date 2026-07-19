export type AuthFieldErrors = {
  email?: string;
  password?: string;
  confirmPassword?: string;
  twoFactorCode?: string;
  captcha?: string;
};

export const isValidEmail = (email: string): boolean => {
  return email.length >= 8 && email.includes("@") && email.includes(".");
};

export const isStrongPassword = (password: string): boolean => {
  if (password.length < 8) {
    return false;
  }

  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSpecial = /[^A-Za-z0-9]/.test(password);

  return hasUpper && hasLower && hasDigit && hasSpecial;
};

export const getPasswordError = (password: string): string | undefined => {
  if (password.length < 8) {
    return "Password must be at least 8 characters";
  }
  if (!/[A-Z]/.test(password)) {
    return "Password must include an uppercase letter";
  }
  if (!/[a-z]/.test(password)) {
    return "Password must include a lowercase letter";
  }
  if (!/\d/.test(password)) {
    return "Password must include a number";
  }
  if (!/[^A-Za-z0-9]/.test(password)) {
    return "Password must include a special character";
  }
  return undefined;
};

export const validateLoginFields = (email: string, password: string): AuthFieldErrors => {
  const errors: AuthFieldErrors = {};

  if (!isValidEmail(email)) {
    errors.email = "Email must be at least 8 characters and contain @ and .";
  }

  if (!isStrongPassword(password)) {
    errors.password = getPasswordError(password) ?? "Password does not meet complexity requirements";
  }

  return errors;
};

export const validateRegisterFields = (
  email: string,
  password: string,
  confirmPassword: string
): AuthFieldErrors => {
  const errors = validateLoginFields(email, password);

  const confirmError = getPasswordError(confirmPassword);
  if (confirmError) {
    errors.confirmPassword = confirmError;
  }

  if (password && confirmPassword && password !== confirmPassword) {
    errors.confirmPassword = "Passwords do not match";
  }

  return errors;
};

export const isLoginFormValid = (email: string, password: string): boolean => {
  return Object.keys(validateLoginFields(email, password)).length === 0;
};

export const isRegisterFormValid = (
  email: string,
  password: string,
  confirmPassword: string
): boolean => {
  return Object.keys(validateRegisterFields(email, password, confirmPassword)).length === 0;
};
