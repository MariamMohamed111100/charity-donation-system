(() => {
  "use strict";

  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  const state = {
    role: null,
    donor: null,
    history: [],
    ngo: null,
    ngoDonations: [],
    adminToken: null,
  };

  const auth = {
    role: "donor",
    mode: "login",
  };

  let selectedNgoId = null;
  let es = null;
  let pollTimer = null;
  let refreshing = false;

  const money = (n) =>
    n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const esc = (s) =>
    String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");

  const TAGLINES = [
    "Where your dollars grow wings 🕊️",
    "Warning: donating may cause spontaneous happiness 😄",
    "Charity, but make it neon 💫",
    "We turned giving into a game you actually win 🎮",
    "Your money teleports to good causes 🚀",
    "100% of funds go to good vibes (and good causes)",
    "No receipts were harmed in the making of this app 🧾",
    "Donating is now as easy as a double-tap 📱",
  ];

  const FACTS = [
    "Did you know? 3 seconds + 1 click = one very happy NGO 🙂",
    "Hearts grown today: statistically uncountable 📈",
    "Pro tip: donations are way cooler than clutter in your wallet 👛",
    "Live update: sparks detected in the dashboard ✨",
    "A dollar a day keeps the 'meh' away 🚫",
    "Fun fact: the cube on the home page is looking at you. It loves you. 🧊",
    "Mission control reports: kindness levels at 3000% 🛰️",
  ];

  const PARALLAX_EMOJIS = ["💫", "🫶", "🎈", "🪙", "🧸", "🌟", "🍀", "🎁", "✨", "💜", "🦋", "🌈"];

  function showView(id) {
    $$(".view").forEach((v) => v.classList.toggle("active", v.id === id));
    window.scrollTo({ top: 0 });
  }

  function toast(msg, type = "info") {
    const box = document.createElement("div");
    box.className = "toast";
    box.innerHTML = (type === "err" ? "⚠️ " : "") + esc(msg);
    $("#toasts").appendChild(box);
    setTimeout(() => {
      box.classList.add("out");
      setTimeout(() => box.remove(), 400);
    }, 3400);
  }

  async function api(path, opts = {}) {
    const o = {
      method: opts.method || "GET",
      headers: { "Content-Type": "application/json" },
    };
    if (state.adminToken) o.headers["X-Admin-Token"] = state.adminToken;
    if (opts.body) o.body = JSON.stringify(opts.body);
    const res = await fetch(path, o);
    let data = null;
    try { data = await res.json(); } catch (e) { data = {}; }
    if (!res.ok && !data.ok) {
      throw Object.assign(new Error(data.error || "Something exploded"), { data });
    }
    return data;
  }

  function countUp(el, target, decimals = 0, dur = 900) {
    const start = performance.now();
    function tick(now) {
      const p = Math.min((now - start) / dur, 1);
      const eased = 1 - Math.pow(1 - p, 3);
      const val = target * eased;
      el.textContent = decimals
        ? val.toLocaleString("en-US", { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
        : Math.round(val).toLocaleString("en-US");
      if (p < 1) requestAnimationFrame(tick);
      else el.textContent = decimals ? target.toLocaleString("en-US", { minimumFractionDigits: decimals, maximumFractionDigits: decimals }) : Math.round(target).toLocaleString("en-US");
    }
    requestAnimationFrame(tick);
  }

  function statCard(icon, label, value, dec, accent) {
    return `<div class="stat-card accent-${accent}">
      <span class="icon">${icon}</span>
      <div class="num" data-count="${value}" data-dec="${dec}">0</div>
      <div class="lbl">${label}</div>
    </div>`;
  }

  function runCountups(scope = document) {
    $$(".num[data-count]", scope).forEach((el) => {
      el.textContent = "0";
      countUp(el, parseFloat(el.dataset.count), parseInt(el.dataset.dec, 10));
    });
  }

  // ---------------- landing ----------------

  function startTextCycler(el, list, interval = 3500) {
    let i = 0;
    const step = () => {
      el.style.animation = "none";
      void el.offsetWidth;
      el.style.animation = "";
      el.textContent = list[i % list.length];
      i++;
    };
    step();
    setInterval(step, interval);
  }

  async function loadLanding() {
    try {
      const s = await api("/api/stats");
      $("#landing-stats").innerHTML = [
        ["🫂", s.donors, "Donors"],
        ["🏢", s.ngos, "NGOs"],
        ["💌", s.totalDonations, "Donations"],
        ["💰", s.totalAmount, "Collected", 2],
      ]
        .map(([ic, v, l, d = 0]) => `<div class="mini-stat"><b>${ic} $${d ? money(v) : v.toLocaleString("en-US")}</b><span>${l}</span></div>`)
        .join("");
    } catch (e) {
      /* server down, keep silent */
    }
  }

  // ---------------- fx ----------------

  function initParallax() {
    const wrap = $("#parallax");
    PARALLAX_EMOJIS.forEach((emoji, i) => {
      const item = document.createElement("span");
      item.className = "px-item";
      item.textContent = emoji;
      item.style.left = Math.random() * 100 + "%";
      item.style.top = Math.random() * 100 + "%";
      item.dataset.depth = (Math.random() * 40 + 10).toFixed(0);
      item.style.fontSize = 1 + Math.random() * 2 + "rem";
      item.dataset.float = Math.random() * 2 * Math.PI;
      wrap.appendChild(item);
      const drift = () => {
        item.style.transform =
          "translate3d(0, " + Math.sin((performance.now() / 3000) + parseFloat(item.dataset.float)) * 14 + "px, 0)";
        requestAnimationFrame(drift);
      };
      if (i < 9) requestAnimationFrame(drift);
    });
    let tx = 0, ty = 0, cx = 0, cy = 0;
    window.addEventListener("mousemove", (e) => {
      cx = (e.clientX / window.innerWidth) - 0.5;
      cy = (e.clientY / window.innerHeight) - 0.5;
    });
    function lerp() {
      tx += (cx - tx) * 0.05;
      ty += (cy - ty) * 0.05;
      $$(".px-item", wrap).forEach((item) => {
        const d = parseFloat(item.dataset.depth);
        item.style.transform +=
          "translate(" + tx * d * 2 + "px, " + ty * d * 2 + "px)";
      });
      requestAnimationFrame(lerp);
    }
    requestAnimationFrame(lerp);
  }

  function initBgCanvas() {
    const canvas = $("#fx-bg");
    const ctx = canvas.getContext("2d");
    let W, H;
    const dots = [];
    function resize() {
      W = canvas.width = innerWidth;
      H = canvas.height = innerHeight;
    }
    resize();
    addEventListener("resize", resize);
    const COLORS = ["255,78,205", "168,85,247", "34,211,238"];
    for (let i = 0; i < 70; i++) {
      dots.push({
        x: Math.random() * innerWidth,
        y: Math.random() * innerHeight,
        r: Math.random() * 2.4 + 0.6,
        s: Math.random() * 0.35 + 0.08,
        c: COLORS[(Math.random() * COLORS.length) | 0],
        o: Math.random() * 0.6 + 0.2,
        tw: Math.random() * Math.PI * 2,
      });
    }
    (function frame() {
      ctx.clearRect(0, 0, W, H);
      dots.forEach((d) => {
        d.y -= d.s;
        d.tw += 0.02;
        if (d.y < -10) { d.y = H + 10; d.x = Math.random() * W; }
        const alpha = d.o * (0.6 + 0.4 * Math.sin(d.tw));
        ctx.beginPath();
        ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${d.c},${alpha})`;
        ctx.shadowColor = `rgba(${d.c},${alpha})`;
        ctx.shadowBlur = 8;
        ctx.fill();
      });
      requestAnimationFrame(frame);
    })();
  }

  const confettiPieces = [];
  function initConfetti() {
    const canvas = $("#fx-confetti");
    const ctx = canvas.getContext("2d");
    let W, H;
    function resize() {
      W = canvas.width = innerWidth;
      H = canvas.height = innerHeight;
    }
    resize();
    addEventListener("resize", resize);
    const COLORS = ["#ff4ecd", "#a855f7", "#22d3ee", "#facc15", "#34d399"];
    (function frame() {
      ctx.clearRect(0, 0, W, H);
      for (let i = confettiPieces.length - 1; i >= 0; i--) {
        const p = confettiPieces[i];
        p.vy += 0.12;
        p.x += Math.sin(p.a) * 1.4;
        p.a += 0.12;
        p.y += p.vy;
        p.rot += p.vr;
        if (p.y > H + 20) { confettiPieces.splice(i, 1); continue; }
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rot);
        ctx.fillStyle = p.c;
        ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
        ctx.restore();
      }
      requestAnimationFrame(frame);
    })();
  }

  function burstConfetti(n = 120) {
    const W = innerWidth;
    for (let i = 0; i < n; i++) {
      confettiPieces.push({
        x: W / 2 + (Math.random() - 0.5) * W * 0.5,
        y: innerHeight * 0.25,
        w: 6 + Math.random() * 8,
        h: 8 + Math.random() * 6,
        vy: -9 - Math.random() * 7,
        a: Math.random() * Math.PI * 2,
        vr: (Math.random() - 0.5) * 0.4,
        rot: Math.random() * Math.PI * 2,
        c: ["#ff4ecd", "#a855f7", "#22d3ee", "#facc15", "#34d399", "#ffffff"][(Math.random() * 6) | 0],
      });
    }
  }

  function hearts(n = 14) {
    for (let i = 0; i < n; i++) {
      const h = document.createElement("div");
      h.className = "heart-fx";
      h.textContent = ["❤️", "💖", "💝", "🫶", "✨", "💜"][(Math.random() * 6) | 0];
      h.style.left = innerWidth / 2 + (Math.random() - 0.5) * innerWidth * 0.6 + "px";
      h.style.bottom = innerHeight * 0.3 + Math.random() * 40 + "px";
      h.style.fontSize = 1.4 + Math.random() * 2 + "rem";
      h.style.animationDuration = 1.6 + Math.random() * 1.6 + "s";
      h.style.animationDelay = Math.random() * 0.3 + "s";
      document.body.appendChild(h);
      setTimeout(() => h.remove(), 4000);
    }
  }

  function happyBang() {
    burstConfetti();
    hearts();
  }

  // ---------------- auth ----------------

  const AUTH_SPEC = {
    donor: {
      emoji: "💝",
      title: "Donor Portal",
      hint: "Try DON001–DON003, or register a fresh one!",
      fields: {
        login: { f1: ["Donor ID", "DON001"], f2: null, f3: null },
        register: { f1: ["Donor ID", "DON004"], f2: ["Full Name"], f3: ["Email", "you@email.com"] },
      },
    },
    ngo: {
      emoji: "🌍",
      title: "NGO Portal",
      hint: "NGO001 & NGO002 are pre-verified. Register to wait in line 🐢",
      fields: {
        login: { f1: ["NGO ID", "NGO001"], f2: null, f3: null },
        register: { f1: ["NGO ID", "NGO005"], f2: ["NGO Name"], f3: ["Mission"] },
      },
    },
    admin: {
      emoji: "🛡️",
      title: "Admin Portal",
      hint: "admin / admin123 — with great power comes great data 📊",
      fields: {
        login: { f1: ["Username", "admin"], f2: null, f3: ["Password", "•••••••"] },
        register: null,
      },
    },
  };

  function openAuth(role) {
    auth.role = role;
    auth.mode = "login";
    const spec = AUTH_SPEC[role];
    $("#auth-emoji").textContent = spec.emoji;
    $("#auth-title").textContent = spec.title;
    $("#auth-hint").textContent = spec.hint;
    $$(".auth-tab").forEach((t) => {
      const isLogin = t.dataset.auth === "login";
      t.classList.toggle("active", isLogin);
      t.style.display = role === "admin" && !isLogin ? "none" : "";
      t.style.visibility = "";
    });
    const subBtn = $("#auth-submit");
    subBtn.disabled = false;
    subBtn.textContent = "Let's go 🚀";
    $("#auth-form").reset();
    configureAuthFields();
    $("#auth-modal").classList.remove("hidden");
  }

  function closeAuth() {
    $("#auth-modal").classList.add("hidden");
  }

  function configureAuthFields() {
    const spec = AUTH_SPEC[auth.role].fields[auth.mode];
    if (!spec) return;
    const f1w = $("#field-1").closest(".field");
    const f2w = $("#field-2-wrap");
    const f3w = $("#field-3-wrap");
    f1w.style.display = spec.f1 ? "" : "none";
    f2w.style.display = spec.f2 ? "" : "none";
    f3w.style.display = spec.f3 ? "" : "none";

    if (spec.f1) {
      $("#field-1-label").textContent = spec.f1[0];
      $("#field-1").placeholder = spec.f1[1] || "";
      $("#field-1").type = "text";
    }
    if (spec.f2) {
      $("#field-2-label").textContent = spec.f2[0];
      $("#field-2").placeholder = spec.f2[1] || "";
    }
    if (spec.f3) {
      $("#field-3-label").textContent = spec.f3[0];
      $("#field-3").placeholder = spec.f3[1] || "";
      $("#field-3").type = auth.role === "admin" ? "password" : "text";
    }
    $("#auth-submit").textContent = auth.mode === "login" ? "Let's go 🚀" : "Sign me up 🎉";
  }

  async function handleAuth(e) {
    e.preventDefault();
    const g = (id) => $("#" + id).value.trim();
    const f1 = g("field-1"), f2 = g("field-2"), f3 = g("field-3");
    const btn = $("#auth-submit");
    btn.disabled = true;

    try {
      let res;
      const role = auth.role;
      if (role === "donor") {
        res = auth.mode === "login"
          ? await api("/api/login/donor", { method: "POST", body: { id: f1 } })
          : await api("/api/register/donor", { method: "POST", body: { id: f1, name: f2, email: f3 } });
      } else if (role === "ngo") {
        res = auth.mode === "login"
          ? await api("/api/login/ngo", { method: "POST", body: { id: f1 } })
          : await api("/api/register/ngo", { method: "POST", body: { id: f1, name: f2, mission: f3 } });
      } else {
        res = await api("/api/login/admin", { method: "POST", body: { username: f1, password: f3 } });
      }
      if (!res.ok) {
        toast(res.error || "That didn't work 😬", "err");
        buttonReset(btn);
        return;
      }
      toast(res.message || "Success!");
      buttonReset(btn);
      closeAuth();
      enterSession(res);
    } catch (err) {
      toast(err.message, "err");
      buttonReset(btn);
    }
  }

  const buttonReset = (btn) => {
    btn.disabled = false;
    btn.textContent = auth.mode === "login" ? "Let's go 🚀" : "Sign me up 🎉";
  };

  async function enterSession(res) {
    if (res.role === "donor") {
      state.role = "donor";
      state.donor = res.donor;
      state.history = res.history || [];
      connectSSE();
      showView("view-donor");
      await renderDonor();
    } else if (res.role === "ngo") {
      state.role = "ngo";
      state.ngo = res.ngo;
      state.ngoDonations = res.donations || [];
      state.ngoTotal = res.totalReceived || 0;
      connectSSE();
      showView("view-ngo");
      await renderNGO();
    } else {
      state.role = "admin";
      state.adminToken = res.token;
      connectSSE();
      showView("view-admin");
      await renderAdmin();
    }
  }

  function logout() {
    if (es) { es.close(); es = null; }
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    refreshing = false;
    state.role = null;
    state.donor = null;
    state.ngo = null;
    state.adminToken = null;
    selectedNgoId = null;
    loadLanding();
    showView("view-landing");
  }

  // ---------------- SSE ----------------

  function connectSSE() {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    if (es) es.close();
    es = new EventSource("/api/events");
    es.onmessage = (e) => {
      if (e.data === "refresh") refreshCurrent();
      if (e.data === "unsupported") {
        es.close();
        es = null;
        if (!pollTimer) pollTimer = setInterval(refreshCurrent, 4000);
      }
    };
    es.onerror = () => {
      if (es && es.readyState === EventSource.CLOSED) {
        es.close();
        es = null;
        if (!pollTimer) pollTimer = setInterval(refreshCurrent, 4000);
      }
    };
  }

  async function refreshCurrent() {
    if (refreshing || !state.role) return;
    refreshing = true;
    try {
      if (state.role === "donor") {
        const r = await api("/api/login/donor", { method: "POST", body: { id: state.donor.id } });
        state.donor = r.donor;
        state.history = r.history;
        await renderDonor(true);
      } else if (state.role === "ngo") {
        const r = await api("/api/login/ngo", { method: "POST", body: { id: state.ngo.id } });
        state.ngo = r.ngo;
        state.ngoDonations = r.donations;
        state.ngoTotal = r.totalReceived;
        await renderNGO(true);
      } else if (state.role === "admin") {
        await renderAdmin(true);
      }
    } catch (e) {
    } finally {
      refreshing = false;
    }
  }

  // ---------------- donor ----------------

  function donorMood(total) {
    if (total <= 0) return ["🦸", "Hero in training — make your first move!"];
    if (total < 50) return ["🐛", "The giving bug has bitten you. Welcome!"];
    if (total < 200) return ["🦋", "Leveling up! Your kindness is showing."];
    if (total < 500) return ["🌟", "Local legend. Kids whisper your name."];
    return ["👑", "Full-on giver royalty. We're not worthy!"];
  }

  async function renderDonor(silent) {
    const d = state.donor;
    const [emoji, mood] = donorMood(d.totalDonated);
    $("#donor-user-chip").innerHTML = `${emoji} <b>${esc(d.name)}</b> <span class="muted">(${esc(d.id)})</span>`;
    $("#donor-stat-cards").innerHTML =
      statCard("💖", "Total Donated ($)", d.totalDonated, 2, "pink") +
      statCard("💌", "Donations Made", state.history.length, 0, "cyan") +
      statCard("🤝", "NGOs Supported", new Set(state.history.map(h => h.ngoId)).size, 0, "green") +
      statCard(emoji, mood, 0, 0, "gold");
    const moodCards = $$(".stat-card", $("#donor-stat-cards"));
    const moodNum = $(".num", moodCards[moodCards.length - 1]);
    moodNum.textContent = emoji;
    moodNum.removeAttribute("data-count");
    runCountups($("#donor-stat-cards"));

    const activeTab = $(".tab.active", $("#view-donor"));
    const pane = activeTab ? activeTab.dataset.tab : "dash";
    await Promise.all([
      pane === "dash" && renderDonorDash(silent),
      pane === "donate" && renderDonorDonate(),
      pane === "history" && renderDonorHistory(),
    ]);

    if (silent) {
      const shell = $("#view-donor .tab-pane.active");
      shell.classList.remove("fade-chunk");
      void shell.offsetWidth;
      shell.classList.add("fade-chunk");
    }
  }

  async function renderDonorDash(silent) {
    const d = state.donor;
    const [emoji, mood] = donorMood(d.totalDonated);
    let feed = '<div class="empty">No donations have landed yet. Be the first spark! ✨</div>';
    try {
      const all = await api("/api/donations");
      const recent = (all.donations || []).slice(-6).reverse();
      feed = recent.map((x) => `
        <div class="row">
          <span>${x.status === "Completed" ? "💌" : "🕐"}</span>
          <div class="grow"><b>${esc(x.donorName)} → ${esc(x.ngoName)}</b><div class="sub">${esc(x.date)} • ${esc(x.status)}</div></div>
          <span class="amt grad">$${money(x.amount)}</span>
        </div>`).join("");
    } catch (e) {}
    $("#donor-dash").innerHTML = `
      <div class="card">
        <h3 class="card-title">${emoji} Hey ${esc(d.name.split(" ")[0])}, you're in the DonationVerse!</h3>
        <p class="muted">${mood}</p>
        <p class="muted small" style="margin-top:8px">Everything below updates <b style="color:var(--green)">LIVE</b> thanks to the Observer pattern. Fancy, right? 🧠</p>
      </div>
      <div class="card">
        <h3 class="card-title">📡 Live giving pulse</h3>
        <div class="rows">${feed}</div>
      </div>`;
  }

  async function renderDonorDonate() {
    const list = await api("/api/ngos?verified=1");
    const ngos = list.ngos || [];
    if (!ngos.length) {
      $("#donor-donate").innerHTML = '<div class="empty">No verified NGOs yet. Check back soon! ⏳</div>';
      return;
    }
    $("#donor-donate").innerHTML = `
      <p class="muted small" style="margin-bottom:14px">Hover a card to peek at its savings. Click to open the donation portal. 🚪</p>
      <div class="ngo-grid" id="donor-ngo-grid">${ngos.map(ngoCard).join("")}</div>`;
    $$("#donor-ngo-grid .ngo-card").forEach((c) =>
      c.addEventListener("click", () => selectNgo(c.dataset.id, ngos))
    );
  }

  function ngoCard(n) {
    return `
      <div class="ngo-card" data-id="${esc(n.id)}">
        <div class="ngo-inner">
          <div class="ngo-face ngo-front">
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span style="font-size:1.6rem">${n.verified ? "🏢" : "⏳"}</span>
              <span class="badge ${n.verified ? "ok" : "wait"}">${n.verified ? "VERIFIED" : "PENDING"}</span>
            </div>
            <b>${esc(n.name)}</b>
            <div class="mission">${esc(n.mission)}</div>
            <div class="small muted">${n.donationCount} donation${n.donationCount === 1 ? "" : "s"} received</div>
          </div>
          <div class="ngo-face ngo-back">
            <span style="margin-top:auto">Save the love</span>
            <div class="big grad">$${money(n.totalReceived)}</div>
            <span>collected so far</span>
            <span class="small muted" style="margin-top:auto">Click to donate!</span>
          </div>
        </div>
      </div>`;
  }

  function selectNgo(id, ngos) {
    selectedNgoId = id;
    $$("#donor-ngo-grid .ngo-card").forEach((c) => c.classList.toggle("selected", c.dataset.id === id));
    const n = ngos.find((x) => x.id === id);
    if (n) {
      $("#donate-modal-title").textContent = `Donate to ${n.name}`;
      $("#donate-modal-mission").textContent = n.mission;
      $("#donate-amount").value = "";
      $("#donate-modal").classList.remove("hidden");
      setTimeout(() => $("#donate-amount").focus(), 50);
    }
  }

  function closeDonateModal() {
    $("#donate-modal").classList.add("hidden");
    selectedNgoId = null;
    $$("#donor-ngo-grid .ngo-card").forEach((c) => c.classList.remove("selected"));
  }

  async function doDonate() {
    if (!selectedNgoId) return toast("Pick an NGO first! 🏢", "err");
    const amount = parseFloat($("#donate-amount").value);
    if (!(amount > 0)) return toast("Enter a positive amount, money wiz 🔢", "err");
    const btn = $("#donate-modal .btn");
    btn.disabled = true;
    try {
      const res = await api("/api/donate", {
        method: "POST",
        body: { donorId: state.donor.id, ngoId: selectedNgoId, amount },
      });
      if (!res.ok) { toast(res.error, "err"); return; }
      happyBang();
      toast(res.message + " (+$" + money(amount) + ")");
      state.donor = res.donor;
      state.history = res.history;
      closeDonateModal();
      await renderDonor();
    } catch (err) {
      toast(err.message, "err");
    } finally {
      btn.disabled = false;
    }
  }

  async function renderDonorHistory() {
    let rows = '<div class="empty">No donations yet — go make some moves! 💸</div>';
    try {
      const all = await api("/api/donations?donor=" + encodeURIComponent(state.donor.id));
      const list = all.donations || [];
      if (list.length) {
        rows = list
          .slice()
          .reverse()
          .map((x) => `
            <div class="row">
              <span>${x.status === "Completed" ? "💌" : "🕐"}</span>
              <div class="grow"><b>${esc(x.ngoName)}</b><div class="sub">${esc(x.date)} • ${esc(x.status)}</div></div>
              <span class="amt">$${money(x.amount)}</span>
            </div>`)
          .join("");
      }
    } catch (e) {}
    $("#donor-history").innerHTML = `<div class="rows">${rows}</div>`;
  }

  // ---------------- NGO ----------------

  async function renderNGO(silent) {
    const n = state.ngo;
    $("#ngo-user-chip").innerHTML = `${n.verified ? "🏢" : "⏳"} <b>${esc(n.name)}</b> <span class="muted">(${esc(n.id)})</span>`;
    $("#ngo-stat-cards").innerHTML =
      statCard("💰", "Total Received ($)", state.ngoTotal, 2, "green") +
      statCard("💌", "Donations", state.ngoDonations.length, 0, "cyan") +
      statCard("🎯", "Campaigns", (n.campaigns || []).length, 0, "purple") +
      statCard(n.verified ? "✅" : "⏳", n.verified ? "Verified" : "Awaiting check", n.verified ? 1 : 0, 0, n.verified ? "green" : "gold");
    const ngoCards = $$(".stat-card", $("#ngo-stat-cards"));
    const verdict = $(".num", ngoCards[ngoCards.length - 1]);
    verdict.textContent = n.verified ? "YES" : "PENDING";
    verdict.removeAttribute("data-count");
    runCountups($("#ngo-stat-cards"));

    await Promise.all([renderNGODash(), renderNGODonations(), renderNGOCampaigns()]);

    if (silent) {
      const shell = $("#view-ngo .tab-pane.active");
      shell.classList.remove("fade-chunk");
      void shell.offsetWidth;
      shell.classList.add("fade-chunk");
    }
  }

  async function renderNGODash() {
    const n = state.ngo;
    $("#ngo-dash").innerHTML = `
      <div class="card">
        <h3 class="card-title">${n.verified ? "✅" : "⏳"} ${n.verified ? "You're on air!" : "Still in the queue…"}</h3>
        ${n.verified
          ? '<p class="muted">Donors can now see you and throw money your way. Enjoy the spotlight! 🎤</p>'
          : '<p class="muted">An admin will verify you soon. Meanwhile, brew some tea and wait in style. ☕</p>'}
        <p class="muted small" style="margin-top:10px"><b>Mission:</b> ${esc(n.mission)}</p>
        <p class="muted small"><b>ID:</b> ${esc(n.id)}</p>
      </div>`;
  }

  async function renderNGODonations() {
    let rows = '<div class="empty">No donations yet. Patience, young NGO. 🌱</div>';
    const list = state.ngoDonations || [];
    if (list.length) {
      rows = list
        .slice()
        .reverse()
        .map((x) => `
          <div class="row">
            <span>💌</span>
            <div class="grow"><b>${esc(x.donorName)}</b><div class="sub">${esc(x.date)} • ${esc(x.status)}</div></div>
            <span class="amt">$${money(x.amount)}</span>
          </div>`)
        .join("");
    }
    $("#ngo-donations").innerHTML = `<div class="rows">${rows}</div>`;
  }

  async function renderNGOCampaigns() {
    const n = state.ngo;
    const chips = (n.campaigns || []).length
      ? (n.campaigns || []).map((c) => `<span class="campaign-chip">🎯 ${esc(c)}</span>`).join(" ")
      : '<p class="muted small">No campaigns yet. Dream big! 🌠</p>';
    $("#ngo-campaigns").innerHTML = `
      <div class="card">
        <h3 class="card-title">🎯 Launch a campaign</h3>
        <form class="campaign-form" style="display:flex;gap:10px;flex-wrap:wrap">
          <input id="campaign-input" type="text" placeholder="e.g. Winter blankets drive" style="flex:1;min-width:180px;font-family:inherit;padding:12px 14px;border-radius:12px;border:1px solid var(--stroke);background:rgba(255,255,255,0.06);color:var(--txt);outline:none" />
          <button class="btn btn-primary sm" type="submit">Launch 🚀</button>
        </form>
        <div style="margin-top:16px">${chips}</div>
      </div>`;
    $(".campaign-form", $("#view-ngo")).addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = $("#campaign-input").value.trim();
      if (!name) return;
      try {
        const res = await api("/api/campaign", { method: "POST", body: { ngoId: state.ngo.id, name } });
        if (res.ok) {
          state.ngo.campaigns = res.campaigns || [];
          toast("Campaign launched! The crowd goes wild 🎉");
          await renderNGOCampaigns();
        } else toast(res.error, "err");
      } catch (err) { toast(err.message, "err"); }
    });
  }

  // ---------------- admin ----------------

  async function renderAdmin(silent) {
    const [stats, pending, allDon, donors, ngos] = await Promise.all([
      api("/api/stats"),
      api("/api/ngos?verified=0"),
      api("/api/donations"),
      api("/api/donors"),
      api("/api/ngos?verified=all"),
    ]);

    $("#admin-stat-cards").innerHTML =
      statCard("🫂", "Donors", stats.donors, 0, "cyan") +
      statCard("🏢", "NGOs", stats.ngos, 0, "purple") +
      statCard("✅", "Verified NGOs", stats.verifiedNgos, 0, "green") +
      statCard("⏳", "Pending NGOs", stats.pendingNgos, 0, "gold") +
      statCard("💌", "Donations", stats.totalDonations, 0, "pink") +
      statCard("💰", "Total Collected ($)", stats.totalAmount, 2, "green");
    runCountups($("#admin-stat-cards"));

    $("#admin-pending").innerHTML = (pending.ngos || []).length
      ? pending.ngos.map((n) => `
          <div class="pending-item">
            <span style="font-size:1.5rem">❓</span>
            <div class="grow">
              <b>${esc(n.name)}</b>
              <div class="sub">${esc(n.id)} • ${esc(n.mission)}</div>
            </div>
            <button class="btn btn-primary sm" onclick="window._verify('${esc(n.id)}')">Verify ✅</button>
          </div>`).join("")
      : '<div class="empty">Nothing pending. All NGOs are behaving. Impressive. 🕵️</div>';

    $("#admin-top-ngo").innerHTML = (() => {
      const top = stats.topNgos || [];
      if (!top.length) return '<div class="empty">No donations yet to rank.</div>';
      const max = Math.max(...top.map((t) => t.total));
      return top.map((t) => `
        <div class="bar-item">
          <div style="display:flex;justify-content:space-between">
            <span class="small">${esc(t.name)}</span>
            <span class="small muted">$${money(t.total)}</span>
          </div>
          <div class="bar-track"><div class="bar-fill" data-w="${((t.total / max) * 100).toFixed(1)}"></div></div>
        </div>`).join("");
    })();
    requestAnimationFrame(() =>
      $$("#admin-top-ngo .bar-fill").forEach((b) => (b.style.width = b.dataset.w + "%"))
    );

    const donorRows = (donors.donors || []).map((u) => `
      <div class="row">
        <span>🫂</span>
        <div class="grow"><b>${esc(u.name)}</b><div class="sub">${esc(u.id)} • ${esc(u.email)}</div></div>
        <span class="amt">$${money(u.totalDonated)}</span>
      </div>`).join("");

    const ngoRows = (ngos.ngos || []).map((u) => `
      <div class="row">
        <span>${u.verified ? "🏢" : "⏳"}</span>
        <div class="grow"><b>${esc(u.name)}</b><div class="sub">${esc(u.id)} • ${u.verified ? "verified" : "pending"}</div></div>
        <span class="amt grad">$${money(u.totalReceived || 0)}</span>
      </div>`).join("");

    const donorsBlock = `<div class="card"><h3 class="card-title">🫂 Donors</h3><div class="rows">${donorRows || '<div class="empty">None yet.</div>'}</div></div>`;
    const ngosBlock = `<div class="card"><h3 class="card-title">🏢 NGOs</h3><div class="rows">${ngoRows || '<div class="empty">None yet.</div>'}</div></div>`;
    $("#admin-users").innerHTML = donorsBlock + ngosBlock;

    const allRows = (allDon.donations || []).slice().reverse().map((x) => `
      <div class="row">
        <span>💌</span>
        <div class="grow"><b>${esc(x.donorName)} → ${esc(x.ngoName)}</b><div class="sub">${esc(x.date)} • ${esc(x.status)}</div></div>
        <span class="amt">$${money(x.amount)}</span>
      </div>`).join("");
    $("#admin-donations").innerHTML = `<div class="rows">${allRows || '<div class="empty">No donations yet.</div>'}</div>`;

    window._verify = async (ngoId) => {
      try {
        const res = await api("/api/verify/ngo", { method: "POST", body: { ngoId } });
        if (res.ok) {
          happyBang();
          toast(res.message);
          await renderAdmin();
        } else toast(res.error, "err");
      } catch (err) { toast(err.message, "err"); }
    };

    if (silent) {
      const shell = $("#view-admin .tab-pane.active");
      shell.classList.remove("fade-chunk");
      void shell.offsetWidth;
      shell.classList.add("fade-chunk");
    }
  }

  // ---------------- wiring ----------------

  function wireTabs() {
    $$(".tabs").forEach((tabs) => {
      tabs.addEventListener("click", (e) => {
        const btn = e.target.closest(".tab");
        if (!btn) return;
        const section = tabs.closest(".view");
        $$(".tab", section).forEach((t) => t.classList.toggle("active", t === btn));
        $$(".tab-pane", section).forEach((p) => p.classList.toggle("active", p.dataset.pane === btn.dataset.tab));
        const pane = btn.dataset.tab;
        if (section.id === "view-donor") {
          if (pane === "donate") renderDonorDonate();
          else if (pane === "history") renderDonorHistory();
        }
      });
    });
  }

  function wireAuthTabs() {
    $$(".auth-tab").forEach((t) =>
      t.addEventListener("click", () => {
        if (auth.role === "admin") return;
        auth.mode = t.dataset.auth;
        $$(".auth-tab").forEach((x) => x.classList.toggle("active", x === t));
        const subBtn = $("#auth-submit");
        subBtn.disabled = false;
        subBtn.textContent = auth.mode === "login" ? "Let's go 🚀" : "Sign me up 🎉";
        configureAuthFields();
        $("#auth-form").reset();
      })
    );
  }

  document.querySelector("#auth-form").addEventListener("submit", handleAuth);
  document.querySelector("#donate-modal").addEventListener("click", (e) => {
    if (e.target.id === "donate-modal") closeDonateModal();
  });
  document.querySelector("#auth-modal").addEventListener("click", (e) => {
    if (e.target.id === "auth-modal") closeAuth();
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      closeAuth();
      closeDonateModal();
    }
  });

  window._verify = null;

  // ---------------- boot ----------------

  initParallax();
  initBgCanvas();
  initConfetti();
  wireTabs();
  wireAuthTabs();
  startTextCycler($("#tagline"), TAGLINES, 3200);
  startTextCycler($("#fun-ticker"), FACTS, 4200);
  loadLanding();

  window.doDonate = doDonate;
  window.handleAuth = handleAuth;
  window.openAuth = openAuth;
  window.closeAuth = closeAuth;
  window.closeDonateModal = closeDonateModal;
  window.logout = logout;
})();