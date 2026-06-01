# Backend Handoff: Android cannot connect to auth API

## Context
Android app sends auth requests to:
- `POST /auth/register`
- `POST /auth/login`
Body:
```json
{
  "login": "string",
  "password": "string"
}
```

Mobile client works by contract from `API_V1.md`. Current issue is network reachability from real phone.

## What backend side must ensure
1. API process is running and listens on `0.0.0.0:8080` (not only `127.0.0.1`).
2. Docker publishes host port 8080 to container port 8080 (`-p 8080:8080` or equivalent in compose).
3. Host firewall allows inbound TCP 8080 from local network.
4. Phone and backend host are in same LAN/Wi-Fi (for local dev).

## Quick verification commands
Run on backend host:

```bash
# container mapping must include 0.0.0.0:8080->8080/tcp
docker ps

# optional: check open port
# Linux/macOS:
ss -ltnp | grep 8080
# Windows (PowerShell):
netstat -ano | findstr :8080
```

From another device in same network (or Android browser):

```bash
curl -i http://<HOST_LAN_IP>:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"test","password":"test"}'
```

Expected:
- reachable TCP connection (no timeout/refused)
- HTTP response (200/400/401 is fine for transport test)

## Notes for Android side
- Emulator URL: `http://10.0.2.2:8080`
- Real phone URL: `http://<HOST_LAN_IP>:8080`
- Production should use public `https://` domain, not local LAN IP
