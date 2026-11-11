import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.PASSWORD || 'DemoPass123!';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<1200'],
  },
};

function authHeaders(token) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}

export default function () {
  const login = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({
    email: 'admin@opus-demo.test',
    password: PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });
  const token = login.json('data.accessToken');

  const stepUp = http.post(`${BASE}/api/v1/auth/step-up`, JSON.stringify({ password: PASSWORD }), authHeaders(token));
  check(stepUp, { 'step-up 200': (r) => r.status === 200 });
  const reauth = stepUp.json('data.accessToken') || token;

  const customers = http.get(`${BASE}/api/v1/customers?size=1`, authHeaders(reauth));
  const products = http.get(`${BASE}/api/v1/products?size=1`, authHeaders(reauth));
  const customerId = customers.json('data.content.0.id');
  const productId = products.json('data.content.0.id');

  const created = http.post(`${BASE}/api/v1/sales-orders`, JSON.stringify({
    customerId,
    items: [{ productId, quantity: 1, unitPrice: 10 }],
  }), authHeaders(reauth));
  check(created, { 'create SO': (r) => r.status === 201 || r.status === 200 });
  const soId = created.json('data.id');

  http.patch(`${BASE}/api/v1/sales-orders/${soId}/status`, JSON.stringify({ status: 'CONFIRMED' }), authHeaders(reauth));
  const fulfilled = http.patch(`${BASE}/api/v1/sales-orders/${soId}/status`, JSON.stringify({ status: 'FULFILLED' }), authHeaders(reauth));
  check(fulfilled, {
    'fulfil 200 or 422': (r) => r.status === 200 || r.status === 422,
  });
  sleep(1);
}
