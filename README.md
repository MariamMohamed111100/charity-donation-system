# DonationVerse 🪐 — Where Giving Gets Fun

A full-stack **charity donation platform** with a playful, animated, modern 3D web UI and a tiny JSON API.
Built as the **web deployment** of a Software Engineering course project whose core is a Java **MVC** system
(`com.donationmvc.*`).

![roles](https://img.shields.io/badge/roles-Donor%20%E2%80%A2%20NGO%20%E2%80%A2%20Admin-ff4ecd)
![api](https://img.shields.io/badge/api-Node.js%20zero--deps-22d3ee)
![ui](https://img.shields.io/badge/ui-glassmorphism%20%2B%203D-a855f7)

---

## ✨ Features

- 🎮 Playful dark-neon UI — animated gradient blobs, rotating CSS 3D cube, mouse parallax
- 💝 **Donor**: live giving feed, verified-NGO cards (3D flip), one-tap donations with **confetti + flying hearts**
- 🏢 **NGO**: mission dashboard, received donations, campaign manager
- 🛡️ **Admin**: live stats, pending-NGO verification, users list, donation ledger, NGO leaderboard
- ⚡ **Live updates**: Server-Sent Events locally; automatically falls back to 4s polling on serverless (Vercel)
- 🧠 Clean layered design — API mirrors the original Java MVC + Repository + Observer logic

## 🔑 Test accounts (sample data seeds on boot)

| Role | Accounts |
|---|---|
| Donor | `DON001` Alice · `DON002` Bob · `DON003` Charlie |
| NGO (verified) | `NGO001` Save the Children · `NGO002` Red Cross |
| NGO (pending) | `NGO003` World Food Program · `NGO004` Team Whiskers |
| Admin | `admin` / `admin123` |

## 🧱 How it maps to the Java project

| Layer | Java project (`com/donationmvc/`) | This repo (web deploy) |
|---|---|---|
| Model + Repository | `com.donationmvc.model.*` / `.repository.*` | `router.js` (in-memory state) |
| REST API endpoints | `com.donationmvc.web.ApiServer` | `api/[...path].js` + `router.js` |
| Views (console → browser) | `com.donationmvc.view.*` | `public/` (HTML/CSS/JS) |
| Observer / live updates | `com.donationmvc.observer.*` | SSE broadcast, polling fallback |

The original console Java app still runs with `java App` (from the repo root), and the local HTTP version with `java WebApp`.

## 🚀 Run locally

```bash
npm start          # → http://localhost:8081
```

or

```bash
node server.js
```

Use the `PORT` env var to change the port.

## ☁️ Deploy to Vercel

1. Push this folder to a GitHub repository.
2. Import that repository at [vercel.com](https://vercel.com) → **Deploy**.
3. Vercel automatically serves `public/` as static assets and `api/` as serverless functions.

> **Note:** serverless state is in-memory — data resets on cold starts. For persistent data,
> attach Vercel KV / Postgres (easy next step).

## 🗂 Project layout

```
api/[...path].js   Vercel catch-all serverless function
router.js          shared API logic (same routes/shapes as the Java server)
server.js          local HTTP server (static + API + SSE)
public/            the web app (index.html, css/, js/)
App.java           Java console entry point
WebApp.java        Java local HTTP entry point
com/donationmvc/   original Java MVC source (model, view, controller, ...)
```

## ⚙️ Configuration

The admin credentials and token fall back to the demo values (`admin` / `admin123` / `s3cr3t-adm1n`).
Override them for any real deployment via env vars: `ADMIN_USER`, `ADMIN_PASSWORD`, `ADMIN_TOKEN`.

## 📚 Related

- Java MVC source + console app: the `src/` folder of the original module (Software Engineering, ECU).
