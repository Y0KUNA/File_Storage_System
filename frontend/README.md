# File Storage Frontend

Static SPA for the File Storage gateway.

## Run locally

```powershell
node server.mjs
```

Open `http://localhost:5173`.

The app calls `http://localhost:8080/api/v1` by default. Override it in DevTools:

```js
localStorage.setItem("apiBase", "http://localhost:8080/api/v1")
```
