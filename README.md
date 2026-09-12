# DonationVerse 🪐 — Software Engineering Design Demo

A full-stack **charity donation platform** built as a **Software Engineering** course project.
The focus of this repo is not "backend" — it is **how the software is designed**:
a clean **layered MVC architecture**, well-defined **design patterns**, and a system that
can ship **3 interchangeable frontends** (console, local web, serverless web) against
**the same domain logic**.

> The exact same use cases are delivered 3 ways: `java App` (console), `java WebApp` (local HTTP),
> and the Node/Vercel web deploy — proving that the presentation layer is fully replaceable.

![roles](https://img.shields.io/badge/roles-Donor%20%E2%80%A2%20NGO%20%E2%80%A2%20Admin-ff4ecd)
![arch](https://img.shields.io/badge/arch-MVC%20%2B%20Layered%20%2B%20Observer-22d3ee)
![pd](https://img.shields.io/badge/design-Observer%20%E2%80%A2%20Repository%20%E2%80%A2%20Listener-a855f7)

---

## 🎯 Why this is a software-engineering project

This project is designed around the engineering skills the course is about — **not just
a working app, but a well-structured one**:

- **Requirements first** — functional and non-functional requirements defined below;
  domain rules (e.g. "donations only to *verified* NGOs") are enforced in the **model layer**,
  never glued inside the console/UI code.
- **Architecture that survives change** — the UI is one thin, swappable layer on top of
  a reusable domain core.
- **Design patterns used deliberately** — Observer, Repository, Listener (interface segregation),
  all with concrete proof in code.
- **Solid OOP** — encapsulation, abstraction, programming-to-interfaces (not to classes).
- **One domain, three frontends** — the ultimate proof of loose coupling.

## 📋 Requirements

### Functional (user stories)
1. As a **Donor**, I can register, browse *verified* NGOs, donate to them, and see my donation history.
2. As an **NGO**, I can register, wait for admin verification, view donations I received, and add campaigns.
3. As an **Admin**, I can log in, verify pending NGOs, and see totals, users, the donation ledger, and the NGO leaderboard.
4. The system **notifies all open views automatically** when data changes.

### Non-functional
- **Maintainability** — each class has one job (single responsibility), so a change in storage
  or UI never ripples through the whole codebase.
- **Extensibility** — observers/listeners let new views be added without touching the models.
- **Portability of the domain** — the same business rules run on the JVM *and* are mirrored by
  the Node/Vercel API for the web deliverable.
- **Usability** — the web UI adds a playful animated layer (glassmorphism + 3D) on top of the
  same API; the console version stays 100% functional.
- **Zero external dependencies** for the Node web layer.

## 👥 Actors & architecture

| Actor   | Responsibilities |
|---------|------------------|
| Donor   | register, browse verified NGOs, donate, view history |
| NGO     | register, manage campaigns, view received donations |
| Admin   | login, verify NGOs, view stats / users / ledger / leaderboard |

```
            ┌─────────────────────────────────────────────┐
            │        PRESENTATION (swappable layer)        │
            │  console views · Java web · Node web (SSE)   │
            └──────────────────────┬──────────────────────┘
                                   │  Listener interfaces
            ┌──────────────────────▼──────────────────────┐
            │            CONTROLLER (flow rules)           │
            │  UserController · DonationController ·       │
            │  AdminController (Node: router.js)           │
            └──────────────────────┬──────────────────────┘
                                   │  subject / observer
            ┌──────────────────────▼──────────────────────┐
            │        MODEL (domain + business rules)       │
            │  UserModel · DonationModel · Donor · NGO ·   │
            │  Donation · Admin                             │
            └──────────────────────┬──────────────────────┘
                                   │  data access
            ┌──────────────────────▼──────────────────────┐
            │        REPOSITORY (storage behind an api)    │
            │  UserRepository · DonationRepository ·       │
            │  AdminRepository                              │
            └─────────────────────────────────────────────┘
```

## 🧩 Design patterns applied

| Pattern       | Where | Why / payoff |
|---------------|-------|-------------|
| **MVC** | `model/` ↔ `controller/` ↔ `view/` | Separation of concerns: data, orchestration and display are independent. |
| **Layered architecture** | Presentation → Controller → Model → Repository | Dependencies point one way (inward); the UI layer is replaceable — proven by shipping 3 frontends. |
| **Observer** | `Subject` interface; models are subjects, views are observers (`UserModel`, `DonationView`, `AdminView`, …) | Views auto-refresh on change; adding a new view needs **zero model changes** (Open/Closed). |
| **Repository** | `UserRepository`, `DonationRepository`, `AdminRepository` | Storage details (in-memory `ArrayList`) are encapsulated; swapping to a DB won't touch controllers/views. |
| **Listener / interface segregation** | `AddUserListener`, `AddDonationListener`, `AdminActionListener` | Views depend on small *abstract* contracts, not concrete controllers — a compile-time dependency inversion. |

## 🏛 OOP & SOLID

- **Encapsulation** — mutable state is `private` with getters/setters and behaviour methods
  (`donation.complete()`, `ngo.verify()`); no raw field poking from outside.
- **Abstraction / polymorphism** — everything depends on `Subject`/`Observer` and listener
  interfaces; concrete classes are wired at startup (`App.java` / `WebApp.java`).
- **S**ingle responsibility — controllers orchestrate, models hold rules, repositories store,
  views render.
- **O**pen/Closed — new observers/views can be added without modifying the subjects.
- **I**nterfaces — focused listener contracts instead of one "god interface".
- **D**ependency inversion — controllers receive *abstractions* (models/observers/listeners),
  never concrete UI classes.

## 🗂 Repository layout

```
App.java                       Java console entry point (wires MVC, no UI logic inside)
WebApp.java                    Java local HTTP entry point (same models, JSON API)
com/donationmvc/
├── model/                     domain entities + business rules (UserModel, DonationModel, Donor, NGO, Donation, Admin)
│   └── repository/            storage abstraction (UserRepository, DonationRepository, AdminRepository)
├── controller/                flow control (UserController, DonationController, AdminController)
├── view/                      console UI implementing Observer (LoginView, DonorView, NGOView, AdminView)
├── listener/                  small interface contracts (AddUserListener, AddDonationListener, AdminActionListener)
├── observer/                  Subject + Observer
└── web/                       Java JSON REST server (ApiServer) — the Java twin of the Node API
public/                        the web deliverable: index.html, css/, js/   ← what Vercel serves
api/[...path].js + router.js   Node/Vercel REST API mirroring ApiServer's routes/shapes
server.js                      local Node HTTP server (static + API + SSE)
```

## 🔑 Test accounts (sample data seeds on boot)

| Role | Accounts |
|---|---|
| Donor | `DON001` Alice · `DON002` Bob · `DON003` Charlie |
| NGO (verified) | `NGO001` Save the Children · `NGO002` Red Cross |
| NGO (pending) | `NGO003` World Food Program · `NGO004` Team Whiskers |
| Admin | `admin` / `admin123` |

## 🚀 Run locally

Java (the full MVC core):

```bash
java App          # console version
java WebApp       # local HTTP version → http://localhost:8080
```

Web (Node mirror):

```bash
npm start         # → http://localhost:8081   (PORT env var changes it)
```

## ☁️ Deploy to Vercel

1. Push this folder to a GitHub repository.
2. Import that repository at [vercel.com](https://vercel.com) → **Deploy**.
3. Vercel automatically serves `public/` as static assets and `api/` as serverless functions.

> Serverless state is in-memory — data resets on cold starts. See "Known limits" below.

## ⚙️ Configuration

The admin credentials/token fall back to demo values (`admin` / `admin123` / `s3cr3t-adm1n`).
Override via env vars: `ADMIN_USER`, `ADMIN_PASSWORD`, `ADMIN_TOKEN` (copy `.env.example` → `.env`).

## 🔮 Known limits / future work

- In-memory storage only → add a real DB behind the Repository interface (that's exactly what
  the pattern is for).
- Admin password is plaintext → store a salted hash.
- Donation status is a `String` → replace with an `enum`.
- No automated tests yet → add unit tests per layer (Repository → Model → Controller).