# SMB Share website

Static HTML, CSS, JavaScript, screenshots and official APK downloads. Production: https://smb.timmyagentic.si/.

The Cloudflare Worker `smb-share-site` serves this directory through the `smb.timmyagentic.si` Custom Domain. The former DNS-only CNAME pointed to `custom-domains.chatgpt.site`; a Worker route alone cannot intercept requests still reaching that origin. The legacy `.openai/hosting.json` is historical metadata; do not publish new versions to that unavailable Sites project. The migration preserves every `/downloads/` file, including the older APK, without rebuilding or re-signing it.

## Verification and deployment

```sh
node --check website/site.js
python3 -m http.server 8787 --directory website
npx wrangler deploy --config website/wrangler.jsonc --dry-run
```

Check desktop and 390 px layouts, direct APK download, the first-use setup link, the desktop QR and screenshot dialogs. Deploy only after the PR review/merge gates. A temporary staging config with no routes and a distinct Worker name can verify assets before a domain cutover.

```sh
npx wrangler deploy --config website/wrangler.jsonc
npx wrangler deployments list --config website/wrangler.jsonc
```

Read back HTTPS, active deployment, CSS/JS, images, robots, sitemap, 404, and the SHA-256 of both APK download paths and SHA256SUMS.txt. Record the original Worker routes and public DNS baseline before activation. Rollback removes this Custom Domain and restores the former DNS-only CNAME `smb` → `custom-domains.chatgpt.site` (observed TTL 600). Remove any task-created trial Worker route after the Custom Domain is verified. Never change device settings, SMB shares, accounts, or historical files as part of website deployment.
