# MOIDA Backend Monitoring

## Uptime Kuma

Create an HTTP monitor for the backend health endpoint:

```text
https://<backend-domain>/api/public/health
```

If the backend is monitored directly from the EC2 instance or private network, use:

```text
http://<backend-host>:9000/api/public/health
```

Recommended monitor settings:

- Interval: 60 seconds
- Retries: 2
- Accepted status codes: 200
- Notification: Discord webhook

The current health endpoint verifies that the Spring application is responding. If database-level health checks are needed later, add Spring Boot Actuator and monitor `/actuator/health`.

## Load Balancer Instance Check

When running multiple backend instances, assign a unique `INSTANCE_NAME` environment variable to each instance.

Open the backend health URL in the browser, then check DevTools > Network:

- Response body includes `instance`.
- Response headers include `X-Instance-Name`.

All backend API responses include the same `X-Instance-Name` header, so you can also inspect normal API requests such as product, category, login, or bid requests.
