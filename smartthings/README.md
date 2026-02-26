# SmartThings app registration update files

App ID already created:
- `d5d77f6e-c777-48db-bf38-540f2bfa992e`

## 1) Edit placeholders

In `app-update.json`:
- Replace `https://YOUR_DOMAIN/smartboiler/webhook` with your real webhook URL.

In `oauth-update.json`:
- Keep `smartboiler://oauth/callback` for Android deep-link testing **only if SmartThings accepts custom scheme redirect URIs for your app type**.
- If custom scheme is rejected, switch to an HTTPS redirect URI that you control.

## 2) Apply via CLI

```powershell
& "$env:TEMP\smartthings-cli-standalone\smartthings.exe" apps:update d5d77f6e-c777-48db-bf38-540f2bfa992e -i .\smartthings\app-update.json -j
& "$env:TEMP\smartthings-cli-standalone\smartthings.exe" apps:oauth:update d5d77f6e-c777-48db-bf38-540f2bfa992e -i .\smartthings\oauth-update.json -j
```

## 3) Verify

```powershell
& "$env:TEMP\smartthings-cli-standalone\smartthings.exe" apps d5d77f6e-c777-48db-bf38-540f2bfa992e -j
& "$env:TEMP\smartthings-cli-standalone\smartthings.exe" apps:oauth d5d77f6e-c777-48db-bf38-540f2bfa992e -j
```
