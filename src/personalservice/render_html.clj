(ns personalservice.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout ledger seq 6): this repo previously had NO demo page and
  no generator at all. This namespace drives the REAL actor stack
  (`personalservice.operation` -> `personalservice.governor` ->
  `personalservice.store`) through a scenario adapted from this repo's
  own `personalservice.sim` demo driver (`clojure -M:dev:run`, confirmed
  to run correctly against the real seeded client directory before this
  file was written -- unlike `cloud-itonami-isic-851`'s `schoolops.sim`,
  this repo's own sim driver uses ids (client-1/2/3/4) that DO match
  `personalservice.store/demo-data`, so it was safe to reuse rather than
  author from scratch), trimmed to a representative subset (one full
  intake->verify->screen->finalize lifecycle ending in a real referral
  finalization, and three distinct HARD-hold reasons) and rendered
  deterministically -- no invented numbers, no timestamps in the page
  content, byte-identical across reruns against the same seed (verified
  by diffing two consecutive runs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [personalservice.store :as store]
            [personalservice.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :provider-staff :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach: client-1 clears intake (auto-commit at phase 3,
  no capital risk), a service-plan verification citing JPN's official
  spec-basis (escalates for phase-3 human approval -- not in the
  phase's :auto set -- approved), a background-check screening that
  comes back cleared (escalates for approval -- approved), and finally
  an :actuation/finalize-referral -- the ONE real-world act this actor
  performs, which ALWAYS escalates (two independent layers agree:
  `personalservice.governor`'s high-stakes gate AND
  `personalservice.phase`'s permanent absence from every phase's :auto
  set) -- approved, producing a real referral number via
  `personalservice.registry`. client-3 gets its service-plan verified
  and approved (evidence complete) but then HARD-holds finalizing a
  referral on this actor's own genuinely-new check: its own recorded 1
  day since contract signing falls short of the 3-day minimum
  cooling-off period. client-2 (jurisdiction \"ATL\", no official
  spec-basis registered in `personalservice.facts`) HARD-holds
  verifying a service plan rather than inventing a jurisdiction's
  requirements. client-4 (seeded with an uncleared background check)
  HARD-holds screening. All three HARD holds never reach a human.
  Returns the resulting store -- every field read by `render` below is
  real governor/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "c1-intake" {:op :client/intake :subject "client-1"
                               :patch {:id "client-1" :client-name "Sato Kenji" :status :active}})

    (exec! actor "c1-serviceplan" {:op :serviceplan/verify :subject "client-1"})
    (approve! actor "c1-serviceplan")

    (exec! actor "c1-bgcheck" {:op :background-check/screen :subject "client-1"})
    (approve! actor "c1-bgcheck")

    (exec! actor "c1-finalize" {:op :actuation/finalize-referral :subject "client-1"})
    (approve! actor "c1-finalize")

    (exec! actor "c3-serviceplan" {:op :serviceplan/verify :subject "client-3"})
    (approve! actor "c3-serviceplan")

    (exec! actor "c3-finalize" {:op :actuation/finalize-referral :subject "client-3"})

    (exec! actor "c2-serviceplan" {:op :serviceplan/verify :subject "client-2"})

    (exec! actor "c4-bgcheck" {:op :background-check/screen :subject "client-4"})
    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for [ledger client-id]
  (last (filter #(= (:subject %) client-id) ledger)))

(defn- status-cell [ledger client-id]
  (let [f (last-fact-for ledger client-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (-> f :violations first :rule)]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- client-row [ledger {:keys [id client-name jurisdiction background-check-not-cleared?
                                  referral-finalized? referral-number]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc client-name) (esc jurisdiction)
          (if background-check-not-cleared?
            "<span class=\"critical\">not cleared</span>"
            "<span class=\"ok\">cleared</span>")
          (if referral-finalized?
            (str "<span class=\"ok\">finalized &middot; " (esc referral-number) "</span>")
            "<span class=\"muted\">not finalized</span>")
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map name) (str/join ", ")) (some-> disposition name) ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own closed op contract
  ;; (README `Ops`, `personalservice.governor`/`personalservice.phase`)
  ;; -- documentation of fixed behavior, not runtime telemetry, so it
  ;; is legitimately hand-described rather than derived from a live run.
  ["        <tr><td><code>:client/intake</code></td><td><span class=\"ok\">phase-3 auto when clean, no capital risk</span></td></tr>"
   "        <tr><td><code>:serviceplan/verify</code></td><td><span class=\"warn\">phase-3 human approval &middot; spec-basis required, never invented</span></td></tr>"
   "        <tr><td><code>:background-check/screen</code></td><td><span class=\"warn\">phase-3 human approval &middot; never auto, any phase</span></td></tr>"
   "        <tr><td><code>:actuation/finalize-referral</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto, any phase &middot; cooling-off period independently recomputed</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        clients (->> (store/all-clients db)
                     (filter #(#{"client-1" "client-2" "client-3" "client-4"} (:id %)))
                     (sort-by :id))
        client-rows (str/join "\n" (map (partial client-row ledger) clients))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-9609 &middot; other personal-service activities</title><style>\n"
     "table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
     ".ok { color: #137a3f; }\n"
     "body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }\n"
     "header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }\n"
     "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }\n"
     "h2 { margin-top: 0; font-size: 15px; }\n"
     ".warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }\n"
     "main { max-width: 980px; margin: 24px auto; padding: 0 20px; }\n"
     "header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }\n"
     ".muted { color: #888; font-size: 13px; }\n"
     ".critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n"
     ".card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n"
     ".err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }\n"
     "th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }\n"
     "header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }\n"
     "code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }\n"
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Other personal-service activities (ISIC 9609) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · referral finalization always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Clients</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>personalservice.store</code> via <code>personalservice.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Client</th><th>Name</th><th>Jurisdiction</th><th>Background check (seed)</th><th>Referral</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     client-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Personal Service Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. A jurisdiction's requirements are never invented; an uncleared background check always blocks; the cooling-off period is independently recomputed from the client's own recorded elapsed days, never trusted from the proposal.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Client</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/referral-history db)) "referral finalizations )")))
