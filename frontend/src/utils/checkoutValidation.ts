export type CheckoutFieldErrors = {
  fullName?: string;
  email?: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  postalCode?: string;
  country?: string;
  paymentMethod?: string;
  deliveryOptionId?: string;
};

export type CheckoutFormValues = {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  postalCode: string;
  country: string;
  paymentMethod: string;
  deliveryOptionId: number | '';
};

const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_DIGITS = /^\+?[0-9]{8,15}$/;
const POSTAL_PATTERN = /^[A-Za-z0-9][A-Za-z0-9\s-]{1,11}$/;
const ADDRESS_PATTERN = /^[\p{L}\p{M}\p{N}\s.,'#/\\-]{3,255}$/u;
const CITY_PATTERN = /^[\p{L}\p{M}][\p{L}\p{M}\s.'(),/-]{1,79}$/u;
const COUNTRY_PATTERN = /^[\p{L}\p{M}][\p{L}\p{M}\s.-]{1,55}$/u;

export const normalizeCheckoutText = (value: string): string =>
  value
    .replace(/\u00a0|\u202f|\u2007|\ufeff/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

export const normalizePhone = (value: string): string =>
  normalizeCheckoutText(value).replace(/[().\-\s]/g, '');

export const validateCheckoutForm = (values: CheckoutFormValues): CheckoutFieldErrors => {
  const errors: CheckoutFieldErrors = {};
  const fullName = normalizeCheckoutText(values.fullName);
  const email = normalizeCheckoutText(values.email);
  const phone = normalizePhone(values.phone);
  const addressLine1 = normalizeCheckoutText(values.addressLine1);
  const addressLine2 = normalizeCheckoutText(values.addressLine2);
  const city = normalizeCheckoutText(values.city);
  const postalCode = normalizeCheckoutText(values.postalCode);
  const country = normalizeCheckoutText(values.country);

  if (!fullName) {
    errors.fullName = 'Full name is required';
  } else if (fullName.length < 2) {
    errors.fullName = 'Full name must be at least 2 characters';
  }

  if (!email) {
    errors.email = 'Email is required';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Invalid email format';
  }

  if (!phone) {
    errors.phone = 'Phone is required';
  } else if (!PHONE_DIGITS.test(phone)) {
    errors.phone = 'Invalid phone format';
  }

  if (!addressLine1) {
    errors.addressLine1 = 'Address is required';
  } else if (!ADDRESS_PATTERN.test(addressLine1)) {
    errors.addressLine1 = 'Invalid address format';
  }

  if (addressLine2 && !ADDRESS_PATTERN.test(addressLine2)) {
    errors.addressLine2 = 'Invalid address format';
  }

  if (!city) {
    errors.city = 'City is required';
  } else if (!CITY_PATTERN.test(city)) {
    errors.city = 'Invalid city';
  }

  if (!postalCode) {
    errors.postalCode = 'Postal code is required';
  } else if (!POSTAL_PATTERN.test(postalCode)) {
    errors.postalCode = 'Invalid postal code format';
  }

  if (!country) {
    errors.country = 'Country is required';
  } else if (!COUNTRY_PATTERN.test(country)) {
    errors.country = 'Invalid country';
  }

  if (!values.paymentMethod.trim()) {
    errors.paymentMethod = 'Payment method is required';
  } else if (!['STRIPE', 'PAYPAL', 'CARD'].includes(values.paymentMethod)) {
    errors.paymentMethod = 'Invalid payment method';
  }

  if (values.deliveryOptionId === '' || values.deliveryOptionId == null) {
    errors.deliveryOptionId = 'Shipping option is required';
  }

  return errors;
};
