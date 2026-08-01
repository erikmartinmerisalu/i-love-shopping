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
};

const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_PATTERN = /^[+]?[0-9\s()-]{7,20}$/;
const POSTAL_PATTERN = /^[A-Za-z0-9][A-Za-z0-9\s-]{2,11}$/;
const ADDRESS_PATTERN = /^[\p{L}0-9\s.,'/\-]{5,255}$/u;

export const validateCheckoutForm = (values: CheckoutFormValues): CheckoutFieldErrors => {
  const errors: CheckoutFieldErrors = {};

  if (!values.fullName.trim()) {
    errors.fullName = 'Full name is required';
  } else if (values.fullName.trim().length < 2) {
    errors.fullName = 'Full name must be at least 2 characters';
  }

  if (!values.email.trim()) {
    errors.email = 'Email is required';
  } else if (!EMAIL_PATTERN.test(values.email.trim())) {
    errors.email = 'Invalid email format';
  }

  if (!values.phone.trim()) {
    errors.phone = 'Phone is required';
  } else if (!PHONE_PATTERN.test(values.phone.trim())) {
    errors.phone = 'Invalid phone format';
  }

  if (!values.addressLine1.trim()) {
    errors.addressLine1 = 'Address is required';
  } else if (!ADDRESS_PATTERN.test(values.addressLine1.trim())) {
    errors.addressLine1 = 'Invalid address format';
  }

  if (values.addressLine2.trim() && !ADDRESS_PATTERN.test(values.addressLine2.trim())) {
    errors.addressLine2 = 'Invalid address format';
  }

  if (!values.city.trim()) {
    errors.city = 'City is required';
  } else if (values.city.trim().length < 2) {
    errors.city = 'Invalid city';
  }

  if (!values.postalCode.trim()) {
    errors.postalCode = 'Postal code is required';
  } else if (!POSTAL_PATTERN.test(values.postalCode.trim())) {
    errors.postalCode = 'Invalid postal code format';
  }

  if (!values.country.trim()) {
    errors.country = 'Country is required';
  }

  if (!values.paymentMethod.trim()) {
    errors.paymentMethod = 'Payment method is required';
  } else if (!['STRIPE', 'PAYPAL', 'CARD'].includes(values.paymentMethod)) {
    errors.paymentMethod = 'Invalid payment method';
  }

  return errors;
};
