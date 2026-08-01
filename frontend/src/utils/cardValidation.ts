/** Client-side card checks only — PAN/CVV are never sent to our API. */

export type CardFieldErrors = {
  cardNumber?: string;
  expiry?: string;
  cvv?: string;
};

const luhnCheck = (digits: string): boolean => {
  let sum = 0;
  let alternate = false;
  for (let i = digits.length - 1; i >= 0; i -= 1) {
    let n = Number(digits[i]);
    if (Number.isNaN(n)) {
      return false;
    }
    if (alternate) {
      n *= 2;
      if (n > 9) {
        n -= 9;
      }
    }
    sum += n;
    alternate = !alternate;
  }
  return sum % 10 === 0;
};

export const sanitizeCardNumber = (value: string): string => value.replace(/\D/g, '');

export const validateCardFields = (
  cardNumber: string,
  expiry: string,
  cvv: string
): CardFieldErrors => {
  const errors: CardFieldErrors = {};
  const number = sanitizeCardNumber(cardNumber);

  if (!number) {
    errors.cardNumber = 'Card number is required';
  } else if (number.length < 13 || number.length > 19 || !luhnCheck(number)) {
    errors.cardNumber = 'Invalid card number format';
  }

  const expiryMatch = expiry.trim().match(/^(0[1-9]|1[0-2])\s*\/\s*(\d{2})$/);
  if (!expiry.trim()) {
    errors.expiry = 'Expiry date is required';
  } else if (!expiryMatch) {
    errors.expiry = 'Invalid expiry date (use MM/YY)';
  } else {
    const month = Number(expiryMatch[1]);
    const year = 2000 + Number(expiryMatch[2]);
    const now = new Date();
    const expiryDate = new Date(year, month, 0, 23, 59, 59);
    if (expiryDate < now) {
      errors.expiry = 'Card is expired';
    }
  }

  const cvvDigits = cvv.replace(/\D/g, '');
  if (!cvvDigits) {
    errors.cvv = 'CVV is required';
  } else if (cvvDigits.length < 3 || cvvDigits.length > 4) {
    errors.cvv = 'Invalid CVV';
  }

  return errors;
};

/**
 * Map Stripe-style test PANs to sandbox scenarios.
 * Only the scenario token is posted to our servers — never the card number.
 */
export const scenarioFromTestCard = (cardNumber: string): string | null => {
  const number = sanitizeCardNumber(cardNumber);
  switch (number) {
    case '4242424242424242':
      return 'success';
    case '4000000000009995':
      return 'insufficient_funds';
    case '4000000000000002':
      return 'invalid_card';
    case '4000000000000069':
      return 'expired_card';
    case '4000000000000010':
      return 'timeout';
    default:
      return null;
  }
};
