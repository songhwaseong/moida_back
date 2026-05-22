# MOIDA Backend Monitoring

## GitHub Secrets

Add this secret before using Discord notifications:

- `DISCORD_WEBHOOK_URL`: Discord channel webhook URL

The existing deployment workflows also require:

- `AWS_REGION`
- `AWS_ACCOUNT_ID`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `SSH_HOST_1`
- `SSH_HOST_2`
- `SSH_USER`
- `SSH_PRIVATE_KEY`
- `SSH_PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `FRONTEND_ORIGIN`: use a comma-separated value when both apex and www domains are used, for example `https://moida.site,https://www.moida.site`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

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

The deployment workflow injects a different `INSTANCE_NAME` value into each EC2 container:

- EC2 1: `moida-backend-ec2-1`
- EC2 2: `moida-backend-ec2-2`

Open the backend health URL in the browser, then check DevTools > Network:

- Response body includes `instance`.
- Response headers include `X-Instance-Name`.

All backend API responses include the same `X-Instance-Name` header, so you can also inspect normal API requests such as product, category, login, or bid requests.
