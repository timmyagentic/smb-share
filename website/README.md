# SMB Share website

Static HTML, CSS, JavaScript, screenshots and official APK downloads. Production: https://smb.timmyagentic.si/.

The Cloudflare Worker `smb-share-site` serves this directory through the exact `smb.timmyagentic.si/*` route. Existing DNS and the old origin stay unchanged; removing only this Worker route restores the previous origin. The legacy `.openai/hosting.json` is historical metadata; do not publish new versions to that unavailable Sites project. The migration preserves every `/downloads/` file, including the older APK, without rebuilding or re-signing it.

## Verification and deployment

```sh
node --check website/site.js
python3 -m http.server 8787 --directory website
npx wrangler deploy --config website/wrangler.jsonc --dry-run
```

Check desktop and 390 px layouts, prompt copy/fallback, screenshot dialogs, manual setup links and downloads. Deploy only after the PR review/merge gates. A temporary staging config with no routes and a distinct Worker name can verify assets before a domain cutover.

```sh
npx wrangler deploy --config website/wrangler.jsonc
npx wrangler deployments list --config website/wrangler.jsonc
```

Read back HTTPS, active deployment, CSS/JS, images, robots, sitemap, 404, and the SHA-256 of both APK download paths and SHA256SUMS.txt. Record the original Worker routes and public DNS baseline before activation; rollback removes only the newly added exact route. Never change device settings, SMB shares, accounts, or historical files as part of website deployment.
