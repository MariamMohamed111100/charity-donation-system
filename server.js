"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");

(function loadEnv() {
  const envPath = path.join(__dirname, ".env");
  if (!fs.existsSync(envPath)) return;
  for (const line of fs.readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const m = line.match(/^\s*([\w.-]+)\s*=\s*(.*)\s*$/);
    if (!m || line.trim().startsWith("#")) continue;
    if (process.env[m[1]] === undefined) {
      process.env[m[1]] = m[2].replace(/^["']|["']$/g, "");
    }
  }
})();

const { handle } = require("./router");

const PORT = parseInt(process.env.PORT, 10) || 8081;
const PUBLIC_DIR = path.join(__dirname, "public");

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
};

function serveStatic(req, res, url) {
  const u = new URL(url, "http://localhost");
  let rel = u.pathname === "/" ? "index.html" : u.pathname.replace(/^\/+/, "");
  const file = path.resolve(PUBLIC_DIR, rel);
  if (!file.startsWith(PUBLIC_DIR)) {
    res.statusCode = 403;
    return res.end("403");
  }
  fs.readFile(file, (err, data) => {
    if (err) {
      res.statusCode = 404;
      res.setHeader("Content-Type", "text/plain; charset=utf-8");
      return res.end("404 - even kindness needs directions \u{1F9ED}");
    }
    res.statusCode = 200;
    res.setHeader("Content-Type", MIME[path.extname(file).toLowerCase()] || "application/octet-stream");
    res.end(data);
  });
}

const server = http.createServer((req, res) => {
  const url = req.url || "/";
  if (url.startsWith("/api/")) {
    handle(req, res);
  } else {
    serveStatic(req, res, url);
  }
});

server.listen(PORT, () => {
  console.log("=== DONATIONVERSE WEB (Node API) ===");
  console.log("Live at: http://localhost:" + PORT);
});