import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE = __ENV.API_BASE || 'http://localhost:8080/api';
const profile = (__ENV.PROFILE || 'full').toLowerCase();

const checkoutTrend = new Trend('s2_checkout_ms');
const payTrend = new Trend('s2_pay_ms');
const paidOrders = new Counter('s2_paid_orders');
const failedCheckouts = new Counter('s2_failed_checkouts');

const stages =
  profile === 'smoke'
    ? [
        { duration: '20s', target: 3 },
        { duration: '20s', target: 3 },
        { duration: '10s', target: 0 },
      ]
    : [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 10 },
        { duration: '1m', target: 20 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 },
      ];

export const options = {
  stages,
  thresholds: {
    http_req_duration: ['p(95)<5000'],
  },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

export function setup() {
  const productsRes = http.get(`${BASE}/products?page=0&size=20`);
  const deliveryRes = http.get(`${BASE}/delivery-options`);

  if (productsRes.status !== 200 || deliveryRes.status !== 200) {
    throw new Error(
      `Setup failed: products ${productsRes.status}, delivery ${deliveryRes.status}. Is docker compose up?`
    );
  }

  const products = productsRes.json('products') || [];
  const options = deliveryRes.json() || [];
  if (!products.length || !options.length) {
    throw new Error('Setup failed: need at least one product and one delivery option');
  }

  const inStock = products.filter((p) => (p.stockQuantity || 0) > 0);
  const product = (inStock.length ? inStock : products).reduce((best, p) =>
    (p.stockQuantity || 0) > (best.stockQuantity || 0) ? p : best
  );

  return {
    productId: product.id,
    deliveryOptionId: options[0].id,
  };
}

export default function (data) {
  const email = `vu${__VU}.iter${__ITER}@load.estvalgus.example`;

  const cartRes = http.post(
    `${BASE}/cart/items`,
    JSON.stringify({ productId: data.productId, quantity: 1 }),
    jsonHeaders
  );

  const cartOk = check(cartRes, {
    'cart 200': (r) => r.status === 200,
  });
  if (!cartOk) {
    failedCheckouts.add(1);
    sleep(1);
    return;
  }

  const orderRes = http.post(
    `${BASE}/orders`,
    JSON.stringify({
      fullName: 'Load Tester',
      email,
      phone: '+37255551234',
      addressLine1: 'Test Street 1',
      city: 'Tallinn',
      postalCode: '10111',
      country: 'Estonia',
      paymentMethod: 'CARD',
      deliveryOptionId: data.deliveryOptionId,
    }),
    jsonHeaders
  );
  checkoutTrend.add(orderRes.timings.duration);

  const orderOk = check(orderRes, {
    'order 201': (r) => r.status === 201,
  });
  if (!orderOk) {
    failedCheckouts.add(1);
    sleep(1);
    return;
  }

  let orderNumber;
  try {
    orderNumber = orderRes.json('orderNumber');
  } catch (_) {
    failedCheckouts.add(1);
    sleep(1);
    return;
  }

  const payRes = http.post(
    `${BASE}/payments/sandbox/confirm`,
    JSON.stringify({ orderNumber, scenario: 'success' }),
    jsonHeaders
  );
  payTrend.add(payRes.timings.duration);

  const paid = check(payRes, {
    'pay success': (r) => r.status === 200 && r.json('success') === true,
  });
  if (paid) {
    paidOrders.add(1);
  } else {
    failedCheckouts.add(1);
  }

  sleep(1);
}
