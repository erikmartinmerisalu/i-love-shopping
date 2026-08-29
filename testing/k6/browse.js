import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE = __ENV.API_BASE || 'http://localhost:8080/api';
const profile = (__ENV.PROFILE || 'full').toLowerCase();

const homeTrend = new Trend('s1_home_ms');
const listTrend = new Trend('s1_list_ms');
const detailTrend = new Trend('s1_detail_ms');
const suggestTrend = new Trend('s1_suggest_ms');
const errors = new Counter('s1_http_errors');

const stages =
  profile === 'smoke'
    ? [
        { duration: '20s', target: 5 },
        { duration: '20s', target: 5 },
        { duration: '10s', target: 0 },
      ]
    : [
        { duration: '30s', target: 5 },
        { duration: '2m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 200 },
        { duration: '1m', target: 0 },
      ];

export const options = {
  stages,
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.05'],
  },
};

function record(res, trend) {
  trend.add(res.timings.duration);
  if (res.status < 200 || res.status >= 400) {
    errors.add(1);
  }
}

export default function () {
  const roll = Math.random();

  if (roll < 0.2) {
    const res = http.get(`${BASE}/home`);
    record(res, homeTrend);
    check(res, { 'home 200': (r) => r.status === 200 });
  } else if (roll < 0.6) {
    const res = http.get(`${BASE}/products?page=0&size=20`);
    record(res, listTrend);
    check(res, { 'list 200': (r) => r.status === 200 });
  } else if (roll < 0.85) {
    const list = http.get(`${BASE}/products?page=0&size=20`);
    record(list, listTrend);
    let id = 1;
    if (list.status === 200) {
      try {
        const body = list.json();
        if (body.products && body.products.length) {
          id = body.products[0].id;
        }
      } catch (_) {
        /* keep fallback id */
      }
    }
    const detail = http.get(`${BASE}/products/${id}`);
    record(detail, detailTrend);
    check(detail, { 'detail 200': (r) => r.status === 200 });
  } else {
    const res = http.get(`${BASE}/products/suggest?q=lamp`);
    record(res, suggestTrend);
    check(res, { 'suggest 200': (r) => r.status === 200 });
  }

  sleep(1);
}
