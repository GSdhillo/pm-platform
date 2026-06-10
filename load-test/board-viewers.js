import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const PROJECT_ID = 'aaaaaaaa-0000-0000-0000-000000000001';

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '2m',  target: 100 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:board}': ['p(95)<300', 'p(99)<800'],
  },
};

export function setup() {
  const res = http.post(`${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: 'vera@demo.com', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'login ok': (r) => r.status === 200 });
  return { token: res.json('token') };
}

export default function (data) {
  const res = http.get(`${BASE}/api/v1/projects/${PROJECT_ID}/board`, {
    headers: { Authorization: `Bearer ${data.token}` },
    tags: { endpoint: 'board' },
  });
  check(res, {
    'board 200': (r) => r.status === 200,
    'has columns': (r) => r.json('columns') !== undefined,
  });
  sleep(Math.random() * 2 + 1);
}
