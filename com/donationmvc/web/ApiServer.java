package com.donationmvc.web;

import com.donationmvc.model.Donation;
import com.donationmvc.model.DonationModel;
import com.donationmvc.model.Donor;
import com.donationmvc.model.NGO;
import com.donationmvc.model.UserModel;
import com.donationmvc.model.repository.AdminRepository;
import com.donationmvc.observer.Observer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiServer {
    private static final int MAX_SSE = 12;
    private static final String ADMIN_TOKEN = "s3cr3t-adm1n";
    private final UserModel userModel;
    private final DonationModel donationModel;
    private final AdminRepository adminRepo = new AdminRepository();
    private final Path webRoot;
    private final HttpServer server;
    private final SseBroker sse = new SseBroker();
    private final ScheduledExecutorService pinger;

    public ApiServer(int port, UserModel userModel, DonationModel donationModel, Path webRoot) throws IOException {
        this.userModel = userModel;
        this.donationModel = donationModel;
        this.webRoot = webRoot;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", this::handle);
        this.server.setExecutor(Executors.newFixedThreadPool(12));
        this.pinger = Executors.newSingleThreadScheduledExecutor();
        this.userModel.registerObserver(sse);
        this.donationModel.registerObserver(sse);
    }

    public void start() {
        server.start();
        pinger.scheduleAtFixedRate(sse::ping, 15, 15, TimeUnit.SECONDS);
    }

    public void stop() {
        server.stop(0);
        pinger.shutdownNow();
        sse.closeAll();
    }

    public HttpServer getServer() { return server; }

    public static Path defaultWebRoot() {
        return Paths.get(System.getProperty("web.dir", "web")).toAbsolutePath();
    }

    private void handle(HttpExchange ex) {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(ex, path.substring(5));
            } else if (path.equals("/favicon.ico")) {
                ex.sendResponseHeaders(204, -1);
            } else {
                serveStatic(ex, path);
            }
        } catch (Exception e) {
            try {
                send(ex, 500, error("Server hiccup: " + e.getMessage()));
            } catch (IOException ignored) {}
        } finally {
            if (!isSsePath(ex)) ex.close();
        }
    }

    private boolean isSsePath(HttpExchange ex) {
        try {
            return ex.getRequestURI().getPath().equals("/api/events");
        } catch (Exception e) { return false; }
    }

    private void handleApi(HttpExchange ex, String path) {
        try {
            if (path.equals("events")) {
                handleEvents(ex);
                return;
            }
            Map<String, Object> out;
            switch (path) {
                case "register/donor":
                    out = registerDonor(bodyMap(ex)); break;
                case "register/ngo":
                    out = registerNGO(bodyMap(ex)); break;
                case "login/donor":
                    out = loginDonor(bodyMap(ex)); break;
                case "login/ngo":
                    out = loginNGO(bodyMap(ex)); break;
                case "login/admin":
                    out = loginAdmin(bodyMap(ex)); break;
                case "donate":
                    out = donate(bodyMap(ex)); break;
                case "verify/ngo":
                    out = verifyNGO(bodyMap(ex), ex.getRequestHeaders()); break;
                case "campaign":
                    out = addCampaign(bodyMap(ex)); break;
                case "donors":
                    out = listDonors(); break;
                case "ngos":
                    out = listNGOs(query(ex).getOrDefault("verified", "all")); break;
                case "donations":
                    out = listDonations(query(ex)); break;
                case "stats":
                    out = stats(); break;
                default:
                    send(ex, 404, error("Unknown API: /api/" + path));
                    return;
            }
            send(ex, 200, out);
        } catch (Exception e) {
            try {
                send(ex, 400, error(e.getMessage()));
            } catch (IOException ignored) {}
        }
    }

    private void handleEvents(HttpExchange ex) throws IOException {
        if (sse.clients.size() >= MAX_SSE) {
            send(ex, 503, error("Too many live viewers, try again in a moment."));
            return;
        }
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "text/event-stream; charset=utf-8");
        h.set("Cache-Control", "no-cache");
        h.set("Connection", "keep-alive");
        ex.sendResponseHeaders(200, 0);
        OutputStream body = ex.getResponseBody();
        body.write("data: connected\n\n".getBytes(StandardCharsets.UTF_8));
        body.flush();
        sse.register(ex, body);
    }

    private Map<String, Object> registerDonor(Map<String, Object> in) {
        String id = str(in, "id");
        String name = str(in, "name");
        String email = str(in, "email");
        if (id == null || name == null || email == null || id.isBlank() || name.isBlank() || email.isBlank()) {
            return error("ID, name and email are all required. No ghost donors!");
        }
        if (userModel.findDonorById(id).isPresent()) return error("Donor ID already exists!");
        if (userModel.findDonorByEmail(email).isPresent()) return error("That email is already taken!");
        Donor d = userModel.registerDonor(id.trim(), name.trim(), email.trim());
        Map<String, Object> out = obj("ok", true, "message", "Welcome to the giving side, " + d.getName() + "!");
        out.put("donor", donorJson(d));
        return out;
    }

    private Map<String, Object> registerNGO(Map<String, Object> in) {
        String id = str(in, "id");
        String name = str(in, "name");
        String mission = str(in, "mission");
        if (id == null || name == null || mission == null || id.isBlank() || name.isBlank() || mission.isBlank()) {
            return error("ID, name and mission are required. We can't save 'nothing'!");
        }
        if (userModel.findNGOById(id).isPresent()) return error("NGO ID already exists!");
        NGO n = userModel.registerNGO(id.trim(), name.trim(), mission.trim());
        Map<String, Object> out = obj("ok", true, "message", "Registration sent! An admin will verify you soon.");
        out.put("ngo", ngoJson(n));
        return out;
    }

    private Map<String, Object> loginDonor(Map<String, Object> in) {
        String id = str(in, "id");
        Optional<Donor> donor = userModel.findDonorById(id);
        if (!donor.isPresent()) return error("Donor not found. Did you register first? 🧐");
        Map<String, Object> out = obj("ok", true, "role", "donor");
        out.put("donor", donorJson(donor.get()));
        out.put("history", donationsJson(donationModel.getDonorDonations(id)));
        return out;
    }

    private Map<String, Object> loginNGO(Map<String, Object> in) {
        String id = str(in, "id");
        Optional<NGO> ngoOpt = userModel.findNGOById(id);
        if (!ngoOpt.isPresent()) return error("NGO not found. Register first!");
        NGO n = ngoOpt.get();
        Map<String, Object> out = obj("ok", true, "role", "ngo");
        out.put("ngo", ngoJson(n));
        List<Donation> list = donationModel.getNGODonations(id);
        out.put("donations", donationsJson(list));
        out.put("totalReceived", completedTotal(list));
        return out;
    }

    private Map<String, Object> loginAdmin(Map<String, Object> in) {
        String username = str(in, "username");
        String password = str(in, "password");
        if (username == null || password == null || !adminRepo.authenticate(username, password)) {
            return error("Nice try, hacker 🤓 Wrong credentials!");
        }
        return obj("ok", true, "role", "admin", "username", username, "token", ADMIN_TOKEN);
    }

    private Map<String, Object> donate(Map<String, Object> in) {
        String donorId = str(in, "donorId");
        String ngoId = str(in, "ngoId");
        double amount = num(in, "amount");
        if (donorId == null || ngoId == null) return error("donorId and ngoId are required!");
        if (!(amount > 0)) return error("Amount must be positive. $0 isn't very generous 😅");
        Optional<Donor> donorOpt = userModel.findDonorById(donorId);
        if (!donorOpt.isPresent()) return error("Donor not found!");
        Optional<NGO> ngoOpt = userModel.findNGOById(ngoId);
        if (!ngoOpt.isPresent()) return error("NGO not found!");
        NGO ngo = ngoOpt.get();
        if (!ngo.isVerified()) return error("This NGO isn't verified yet. Admin says: wait!");
        donationModel.addDonation(donorId, ngoId, amount);
        userModel.updateDonorDonation(donorId, amount);
        Map<String, Object> out = obj("ok", true, "message", "Donation sent! A heart just grew 3 sizes ❤️");
        out.put("donor", donorJson(donorOpt.get()));
        out.put("ngoTotal", completedTotal(donationModel.getNGODonations(ngoId)));
        out.put("history", donationsJson(donationModel.getDonorDonations(donorId)));
        return out;
    }

    private Map<String, Object> verifyNGO(Map<String, Object> in, Headers headers) {
        if (!ADMIN_TOKEN.equals(first(headers, "X-Admin-Token"))) {
            return error("Unauthorized. Admin clearance required!");
        }
        String ngoId = str(in, "ngoId");
        if (ngoId == null) return error("ngoId is required!");
        if (userModel.verifyNGO(ngoId)) {
            return obj("ok", true, "message", "NGO " + ngoId + " is now verified! 🥳");
        }
        return error("NGO not found!");
    }

    private Map<String, Object> addCampaign(Map<String, Object> in) {
        String ngoId = str(in, "ngoId");
        String name = str(in, "name");
        if (ngoId == null || name == null || name.isBlank()) return error("Campaign needs a name!");
        Optional<NGO> ngoOpt = userModel.findNGOById(ngoId);
        if (!ngoOpt.isPresent()) return error("NGO not found!");
        ngoOpt.get().addCampaign(name);
        return obj("ok", true, "message", "Campaign '" + name + "' added!", "campaigns", ngoOpt.get().getCampaigns());
    }

    private Map<String, Object> listDonors() {
        List<Object> list = new ArrayList<>();
        for (Donor d : userModel.getAllDonors()) list.add(donorJson(d));
        return obj("ok", true, "donors", list);
    }

    private Map<String, Object> listNGOs(String verified) {
        List<Object> list = new ArrayList<>();
        for (NGO n : userModel.getAllNGOs()) {
            if (verified.equals("1") && !n.isVerified()) continue;
            if (verified.equals("0") && n.isVerified()) continue;
            Map<String, Object> m = ngoJson(n);
            m.put("totalReceived", completedTotal(donationModel.getNGODonations(n.getId())));
            list.add(m);
        }
        return obj("ok", true, "ngos", list);
    }

    private Map<String, Object> listDonations(Map<String, String> q) {
        List<Donation> list;
        if (q.containsKey("donor") && !q.get("donor").isEmpty()) {
            list = donationModel.getDonorDonations(q.get("donor"));
        } else if (q.containsKey("ngo") && !q.get("ngo").isEmpty()) {
            list = donationModel.getNGODonations(q.get("ngo"));
        } else {
            list = donationModel.getAllDonations();
        }
        return obj("ok", true, "donations", donationsJson(list));
    }

    private Map<String, Object> stats() {
        List<Object> top = new ArrayList<>();
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Donation d : donationModel.getAllDonations()) {
            if (!d.getStatus().equals("Completed")) continue;
            totals.merge(d.getNgoId(), d.getAmount(), Double::sum);
        }
        totals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> {
                    Map<String, Object> m = obj("id", e.getKey(), "total", e.getValue());
                    userModel.findNGOById(e.getKey()).ifPresent(n -> m.put("name", n.getName()));
                    top.add(m);
                });
        Map<String, Object> out = obj("ok", true);
        out.put("donors", userModel.getAllDonors().size());
        out.put("ngos", userModel.getAllNGOs().size());
        out.put("verifiedNgos", userModel.getVerifiedNGOs().size());
        out.put("pendingNgos", userModel.getPendingVerification().size());
        out.put("totalDonations", donationModel.getTotalDonationsCount());
        out.put("totalAmount", donationModel.getTotalDonationsAmount());
        out.put("topNgos", top);
        return out;
    }

    private Map<String, Object> donorJson(Donor d) {
        return obj("id", d.getId(), "name", d.getName(), "email", d.getEmail(), "totalDonated", d.getTotalDonated());
    }

    private Map<String, Object> ngoJson(NGO n) {
        return obj("id", n.getId(), "name", n.getName(), "mission", n.getMission(),
                "verified", n.isVerified(), "campaigns", new ArrayList<>(n.getCampaigns()),
                "donationCount", donationModel.getNGODonations(n.getId()).size());
    }

    private List<Object> donationsJson(List<Donation> list) {
        List<Object> out = new ArrayList<>();
        for (Donation d : list) {
            Map<String, Object> m = obj("id", d.getId(), "donorId", d.getDonorId(), "ngoId", d.getNgoId(),
                    "amount", d.getAmount(), "date", String.valueOf(d.getDate()), "status", d.getStatus());
            userModel.findDonorById(d.getDonorId()).ifPresent(dr -> m.put("donorName", dr.getName()));
            userModel.findNGOById(d.getNgoId()).ifPresent(n -> m.put("ngoName", n.getName()));
            out.add(m);
        }
        return out;
    }

    private double completedTotal(List<Donation> list) {
        return list.stream().filter(d -> d.getStatus().equals("Completed")).mapToDouble(Donation::getAmount).sum();
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        String rel = path.equals("/") ? "index.html" : path;
        if (rel.startsWith("/")) rel = rel.substring(1);
        Path file = webRoot.resolve(rel).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            send(ex, 404, error("404 — even kindness needs directions 🧭"));
            return;
        }
        byte[] data = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType(file));
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private String contentType(Path file) {
        String n = file.getFileName().toString().toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css")) return "text/css; charset=utf-8";
        if (n.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (n.endsWith(".json")) return "application/json; charset=utf-8";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private void send(HttpExchange ex, int code, Map<String, Object> body) throws IOException {
        byte[] data = Json.encode(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyMap(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (raw.isBlank()) return new HashMap<>();
            Object parsed = Json.parse(raw);
            if (parsed instanceof Map<?, ?>) return (Map<String, Object>) parsed;
            return new HashMap<>();
        }
    }

    private Map<String, String> query(HttpExchange ex) {
        Map<String, String> out = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q == null || q.isEmpty()) return out;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) out.put(dec(pair), "");
            else out.put(dec(pair.substring(0, eq)), dec(pair.substring(eq + 1)));
        }
        return out;
    }

    private String dec(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private static double num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private static String first(Headers headers, String name) {
        List<String> v = headers.get(name);
        return (v == null || v.isEmpty()) ? null : v.get(0);
    }

    private static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    private static Map<String, Object> error(String message) {
        return obj("ok", false, "error", message);
    }

    private static final class SseBroker implements Observer {
        private final CopyOnWriteArrayList<Map.Entry<HttpExchange, OutputStream>> clients = new CopyOnWriteArrayList<>();

        void register(HttpExchange ex, OutputStream os) {
            clients.add(new java.util.AbstractMap.SimpleEntry<>(ex, os));
        }

        @Override
        public void update() {
            broadcast("refresh");
        }

        void ping() {
            broadcast(":ping");
        }

        void broadcast(String data) {
            String msg = "data: " + data + "\n\n";
            for (Map.Entry<HttpExchange, OutputStream> c : clients) {
                try {
                    c.getValue().write(msg.getBytes(StandardCharsets.UTF_8));
                    c.getValue().flush();
                } catch (IOException e) {
                    drop(c);
                }
            }
        }

        void drop(Map.Entry<HttpExchange, OutputStream> c) {
            clients.remove(c);
            try { c.getKey().close(); } catch (Exception ignored) {}
        }

        void closeAll() {
            for (Map.Entry<HttpExchange, OutputStream> c : clients) drop(c);
        }
    }
}