(ns personalservice.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean client through
  intake -> service-plan verification -> background-check screening ->
  referral-finalization proposal (always escalates) -> human approval
  -> commit, then shows four HARD holds (a jurisdiction with no spec-
  basis, a client whose own recorded elapsed days since contract
  signing fall short of the jurisdiction's cooling-off period, an
  uncleared background check screened directly via `:background-check/
  screen` [never via an actuation op against an unscreened client --
  see this actor's own governor ns docstring / the lesson `parksafety`'s
  ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s, `conservation`'s,
  `salon`'s, `entertainment`'s, `casework`'s, `hospital`'s, `facility`'s,
  `school`'s, `association`'s, `leasing`'s, `behavioral`'s, `secondary`'s,
  `card`'s, `water`'s, `telecom`'s, `aerospace`'s, `recovery`'s,
  `consulting`'s, `union`'s, `congregation`'s, `fab`'s, `energy`'s,
  `care`'s, `navigator`'s, `learning`'s, `banking`'s, `advertising`'s,
  `polling`'s, `research`'s, `design`'s, `nursing`'s, `sports`'s,
  `alliedhealth`'s, `laundry`'s, `holdco`'s and `photo`'s ADR-0001s
  already recorded], and a double finalization of an already-processed
  client) that never reach a human at all, and prints the audit ledger
  + the draft referral-finalization records."
  (:require [langgraph.graph :as g]
            [personalservice.store :as store]
            [personalservice.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :provider-staff :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== client/intake client-1 (JPN, clean; cooling-off elapsed, background check cleared) ==")
    (println (exec! actor "t1" {:op :client/intake :subject "client-1"
                                :patch {:id "client-1" :client-name "Sato Kenji"}} operator))

    (println "== serviceplan/verify client-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :serviceplan/verify :subject "client-1"} operator))
    (println (approve! actor "t2"))

    (println "== background-check/screen client-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :background-check/screen :subject "client-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/finalize-referral client-1 (always escalates -- actuation/finalize-referral) ==")
    (let [r (exec! actor "t4" {:op :actuation/finalize-referral :subject "client-1"} operator)]
      (println r)
      (println "-- human provider staff approves --")
      (println (approve! actor "t4")))

    (println "== serviceplan/verify client-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :serviceplan/verify :subject "client-2" :no-spec? true} operator))

    (println "== serviceplan/verify client-3 (escalates -- human approves; sets up the cooling-off test) ==")
    (println (exec! actor "t6" {:op :serviceplan/verify :subject "client-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/finalize-referral client-3 (only 1 day since contract signed < 3-day minimum -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/finalize-referral :subject "client-3"} operator))

    (println "== background-check/screen client-4 (not cleared -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :background-check/screen :subject "client-4"} operator))

    (println "== actuation/finalize-referral client-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/finalize-referral :subject "client-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft referral-finalization records ==")
    (doseq [r (store/referral-history db)] (println r))))
