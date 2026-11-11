import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 15,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<600'],
  },
};

export default function () {
  const login = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({
    email: 'admin@opus-demo.test',
    password: __ENV.PASSWORD || 'DemoPass123!',
  }), { headers: { 'Content-Type': 'application/json' } });
  const token = login.json('data.accessToken');
  const auth = { headers: { Authorization: `Bearer ${token}` } };

  check(http.get(`${BASE}/api/v1/reports/dashboard`, auth), { 'dashboard 200': (r) => r.status === 200 });
  check(http.get(`${BASE}/api/v1/reports/dashboard/low-stock`, auth), { 'low-stock 200': (r) => r.status === 200 });
  sleep(0.5);
}
