"use strict";

const ADMIN_TOKEN = process.env.ADMIN_TOKEN || "s3cr3t-adm1n";
const ADMIN_USER = process.env.ADMIN_USER || "admin";
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || "admin123";
const IS_VERCEL = process.env.VERCEL === "1";

const state = { donors: [], ngos: [], donations: [], nextDonationId: 1 };
const sseClients = [];

function resetData() {
  state.donors = [];
  state.ngos = [];
  state.donations = [];
  state.nextDonationId = 1;

  addDonor("DON001", "Alice Johnson", "alice@email.com");
  addDonor("DON002", "Bob Smith", "bob@email.com");
  addDonor("DON003", "Charlie Brown", "charlie@email.com");

  addNGO("NGO001", "Save the Children", "Helping children in need");
  addNGO("NGO002", "Red Cross", "Emergency relief worldwide");
  addNGO("NGO003", "World Food Program", "Fighting hunger globally");
  addNGO("NGO004", "Team Whiskers", "Rescuing cats one meow at a time");

  verifyNGO("NGO001");
  verifyNGO("NGO002");

  createDonation("DON001", "NGO001", 100);
  createDonation("DON002", "NGO001", 50);
  createDonation("DON003", "NGO002", 200);
  createDonation("DON001", "NGO002", 75);
}

function addDonor(id, name, email) {
  const d = { id, name, email, totalDonated: 0 };
  state.donors.push(d);
  return d;
}

function addNGO(id, name, mission) {
  const n = { id, name, mission, verified: false, campaigns: [] };
  state.ngos.push(n);
  return n;
}

function verifyNGO(id) {
  const n = findNGO(id);
  if (n) { n.verified = true; return true; }
  return false;
}

function createDonation(donorId, ngoId, amount) {
  const donation = {
    id: "DON" + String(state.nextDonationId).padStart(3, "0"),
    donorId,
    ngoId,
    amount,
    date: new Date().toISOString().slice(0, 10),
    status: "Completed",
  };
  state.nextDonationId += 1;
  const donor = findDonor(donorId);
  if (donor) donor.totalDonated += amount;
  state.donations.push(donation);
  return donation;
}

const findDonor = (id) => state.donors.find((d) => d.id === id);
const findDonorByEmail = (email) => state.donors.find((d) => d.email.toLowerCase() === String(email).toLowerCase());
const findNGO = (id) => state.ngos.find((n) => n.id === id);
const ngoDonations = (id) => state.donations.filter((d) => d.ngoId === id);
const donorDonations = (id) => state.donations.filter((d) => d.donorId === id);
const completedTotal = (list) => list.filter((d) => d.status === "Completed").reduce((s, d) => s + d.amount, 0);

function donorJson(d) {
  return { id: d.id, name: d.name, email: d.email, totalDonated: d.totalDonated };
}

function ngoJson(n) {
  return {
    id: n.id,
    name: n.name,
    mission: n.mission,
    verified: n.verified,
    campaigns: n.campaigns.slice(),
    donationCount: ngoDonations(n.id).length,
  };
}

function donationJson(d) {
  const m = {
    id: d.id,
    donorId: d.donorId,
    ngoId: d.ngoId,
    amount: d.amount,
    date: d.date,
    status: d.status,
  };
  const donor = findDonor(d.donorId);
  const ngo = findNGO(d.ngoId);
  if (donor) m.donorName = donor.name;
  if (ngo) m.ngoName = ngo.name;
  return m;
}

function sendJson(res, code, obj) {
  const body = JSON.stringify(obj);
  res.statusCode = code;
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  res.end(body);
}

const error = (message) => ({ ok: false, error: message });

function readBody(req, cb) {
  const chunks = [];
  req.on("data", (c) => chunks.push(c));
  req.on("end", () => {
    const raw = Buffer.concat(chunks).toString("utf8");
    let parsed = {};
    if (raw.trim()) {
      try { parsed = JSON.parse(raw); } catch (e) { parsed = {}; }
    }
    cb(parsed);
  });
}

// ---------- route handlers ----------

function registerDonor(req, res, body) {
  const id = body.id != null ? String(body.id).trim() : "";
  const name = body.name != null ? String(body.name).trim() : "";
  const email = body.email != null ? String(body.email).trim() : "";
  if (!id || !name || !email) {
    return sendJson(res, 200, error("ID, name and email are all required. No ghost donors!"));
  }
  if (findDonor(id)) return sendJson(res, 200, error("Donor ID already exists!"));
  if (findDonorByEmail(email)) return sendJson(res, 200, error("That email is already taken!"));
  const d = addDonor(id, name, email);
  broadcast("refresh");
  sendJson(res, 200, { ok: true, message: "Welcome to the giving side, " + d.name + "!", donor: donorJson(d) });
}

function registerNGO(req, res, body) {
  const id = body.id != null ? String(body.id).trim() : "";
  const name = body.name != null ? String(body.name).trim() : "";
  const mission = body.mission != null ? String(body.mission).trim() : "";
  if (!id || !name || !mission) {
    return sendJson(res, 200, error("ID, name and mission are required. We can't save 'nothing'!"));
  }
  if (findNGO(id)) return sendJson(res, 200, error("NGO ID already exists!"));
  const n = addNGO(id, name, mission);
  broadcast("refresh");
  sendJson(res, 200, { ok: true, message: "Registration sent! An admin will verify you soon.", ngo: ngoJson(n) });
}

function loginDonor(res, body) {
  const id = body.id != null ? String(body.id).trim() : "";
  const donor = findDonor(id);
  if (!donor) return sendJson(res, 200, error("Donor not found. Did you register first? 🧐"));
  sendJson(res, 200, {
    ok: true,
    role: "donor",
    donor: donorJson(donor),
    history: donorDonations(id).map(donationJson),
  });
}

function loginNGO(res, body) {
  const id = body.id != null ? String(body.id).trim() : "";
  const ngo = findNGO(id);
  if (!ngo) return sendJson(res, 200, error("NGO not found. Register first!"));
  const list = ngoDonations(id);
  sendJson(res, 200, {
    ok: true,
    role: "ngo",
    ngo: ngoJson(ngo),
    donations: list.map(donationJson),
    totalReceived: completedTotal(list),
  });
}

function loginAdmin(res, body) {
  const username = body.username != null ? String(body.username) : "";
  const password = body.password != null ? String(body.password) : "";
  if (username !== ADMIN_USER || password !== ADMIN_PASSWORD) {
    return sendJson(res, 200, error("Nice try, hacker 🤓 Wrong credentials!"));
  }
  sendJson(res, 200, { ok: true, role: "admin", username, token: ADMIN_TOKEN });
}

function donate(res, body) {
  const donorId = body.donorId != null ? String(body.donorId) : "";
  const ngoId = body.ngoId != null ? String(body.ngoId) : "";
  const amount = parseFloat(body.amount);
  if (!donorId || !ngoId) return sendJson(res, 200, error("donorId and ngoId are required!"));
  if (!(amount > 0)) return sendJson(res, 200, error("Amount must be positive. $0 isn't very generous 😅"));
  const donor = findDonor(donorId);
  if (!donor) return sendJson(res, 200, error("Donor not found!"));
  const ngo = findNGO(ngoId);
  if (!ngo) return sendJson(res, 200, error("NGO not found!"));
  if (!ngo.verified) return sendJson(res, 200, error("This NGO isn't verified yet. Admin says: wait!"));
  createDonation(donorId, ngoId, amount);
  broadcast("refresh");
  sendJson(res, 200, {
    ok: true,
    message: "Donation sent! A heart just grew 3 sizes ❤️",
    donor: donorJson(donor),
    ngoTotal: completedTotal(ngoDonations(ngoId)),
    history: donorDonations(donorId).map(donationJson),
  });
}

function verifyNgoRoute(req, res, body) {
  if (req.headers["x-admin-token"] !== ADMIN_TOKEN) {
    return sendJson(res, 200, error("Unauthorized. Admin clearance required!"));
  }
  const ngoId = body.ngoId != null ? String(body.ngoId) : "";
  if (!ngoId) return sendJson(res, 200, error("ngoId is required!"));
  if (verifyNGO(ngoId)) {
    broadcast("refresh");
    return sendJson(res, 200, { ok: true, message: "NGO " + ngoId + " is now verified! 🥳" });
  }
  sendJson(res, 200, error("NGO not found!"));
}

function addCampaign(res, body) {
  const ngoId = body.ngoId != null ? String(body.ngoId) : "";
  const name = body.name != null ? String(body.name).trim() : "";
  if (!ngoId || !name) return sendJson(res, 200, error("Campaign needs a name!"));
  const ngo = findNGO(ngoId);
  if (!ngo) return sendJson(res, 200, error("NGO not found!"));
  ngo.campaigns.push(name);
  broadcast("refresh");
  sendJson(res, 200, { ok: true, message: "Campaign '" + name + "' added!", campaigns: ngo.campaigns.slice() });
}

function listDonors(res) {
  sendJson(res, 200, { ok: true, donors: state.donors.map(donorJson) });
}

function listNGOs(res, verified) {
  const list = state.ngos
    .filter((n) => !(verified === "1" && !n.verified))
    .filter((n) => !(verified === "0" && n.verified))
    .map((n) => Object.assign(ngoJson(n), { totalReceived: completedTotal(ngoDonations(n.id)) }));
  sendJson(res, 200, { ok: true, ngos: list });
}

function listDonations(res, query) {
  let list = state.donations;
  if (query.get("donor")) list = donorDonations(query.get("donor"));
  else if (query.get("ngo")) list = ngoDonations(query.get("ngo"));
  sendJson(res, 200, { ok: true, donations: list.map(donationJson) });
}

function stats(res) {
  const totals = {};
  for (const d of state.donations) {
    if (d.status !== "Completed") continue;
    totals[d.ngoId] = (totals[d.ngoId] || 0) + d.amount;
  }
  const top = Object.entries(totals)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([id, total]) => {
      const m = { id, total };
      const ngo = findNGO(id);
      if (ngo) m.name = ngo.name;
      return m;
    });
  sendJson(res, 200, {
    ok: true,
    donors: state.donors.length,
    ngos: state.ngos.length,
    verifiedNgos: state.ngos.filter((n) => n.verified).length,
    pendingNgos: state.ngos.filter((n) => !n.verified).length,
    totalDonations: state.donations.length,
    totalAmount: completedTotal(state.donations),
    topNgos: top,
  });
}

// ---------- SSE ----------

function handleEvents(req, res) {
  if (IS_VERCEL) {
    res.statusCode = 200;
    res.setHeader("Content-Type", "text/event-stream; charset=utf-8");
    res.setHeader("Cache-Control", "no-cache");
    res.end("data: unsupported\n\n");
    return;
  }
  res.statusCode = 200;
  res.setHeader("Content-Type", "text/event-stream; charset=utf-8");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");
  res.write("data: connected\n\n");
  const client = { res };
  sseClients.push(client);
  req.on("close", () => {
    const i = sseClients.indexOf(client);
    if (i >= 0) sseClients.splice(i, 1);
  });
}

function broadcast(data) {
  const msg = "data: " + data + "\n\n";
  for (let i = sseClients.length - 1; i >= 0; i--) {
    const c = sseClients[i];
    try {
      c.res.write(msg);
    } catch (e) {
      sseClients.splice(i, 1);
    }
  }
}

// ---------- dispatcher ----------

function handle(req, res) {
  const url = new URL(req.url, "http://localhost");
  const segs = url.pathname.replace(/^\/+|\/+$/g, "").split("/").filter(Boolean);
  const route = segs.join("/");
  const query = url.searchParams;
  const method = req.method || "GET";

  const routes = {
    "api/register/donor": () => readBody(req, (b) => registerDonor(req, res, b)),
    "api/register/ngo": () => readBody(req, (b) => registerNGO(req, res, b)),
    "api/login/donor": () => readBody(req, (b) => loginDonor(res, b)),
    "api/login/ngo": () => readBody(req, (b) => loginNGO(res, b)),
    "api/login/admin": () => readBody(req, (b) => loginAdmin(res, b)),
    "api/donate": () => readBody(req, (b) => donate(res, b)),
    "api/verify/ngo": () => readBody(req, (b) => verifyNgoRoute(req, res, b)),
    "api/campaign": () => readBody(req, (b) => addCampaign(res, b)),
    "api/events": () => handleEvents(req, res),
  };

  const methodGetRoutes = {
    "api/donors": () => listDonors(res),
    "api/ngos": () => listNGOs(res, query.get("verified") || "all"),
    "api/donations": () => listDonations(res, query),
    "api/stats": () => stats(res),
  };

  if (methodGetRoutes[route]) return methodGetRoutes[route]();
  if (routes[route]) {
    if (method !== "POST" && route !== "api/events") {
      return sendJson(res, 405, error("Method not allowed"));
    }
    return routes[route]();
  }
  sendJson(res, 404, error("Unknown API: /api/" + route.replace(/^api\//, "")));
}

resetData();

module.exports = { handle, resetData, getState: () => state };