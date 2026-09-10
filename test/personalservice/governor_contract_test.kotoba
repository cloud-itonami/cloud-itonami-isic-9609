(ns personalservice.governor-contract-test
  "The governor contract as executable tests -- the personal-service
  analog of `cloud-itonami-isic-6512`'s `casualty.governor-contract-
  test`. The single invariant under test:

    PersonalServiceOps-LLM never finalizes a referral the Personal
    Service Governor would reject, `:actuation/finalize-referral`
    NEVER auto-commits at any phase, `:client/intake` (no direct
    capital risk) MAY auto-commit when clean, and every decision
    (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [personalservice.store :as store]
            [personalservice.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :provider-staff :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a service-plan
  assessment on file. Uses distinct thread-ids per call site by
  suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :serviceplan/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :client/intake :subject "client-1"
                   :patch {:id "client-1" :client-name "Sato Kenji"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sato Kenji" (:client-name (store/client db "client-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest serviceplan-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :serviceplan/verify :subject "client-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/serviceplan-of db "client-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a serviceplan/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :serviceplan/verify :subject "client-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/serviceplan-of db "client-1")) "no service plan written"))))

(deftest finalize-referral-without-serviceplan-is-held
  (testing "actuation/finalize-referral before any service-plan verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/finalize-referral :subject "client-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest cooling-off-period-not-elapsed-is-held
  (testing "a client whose own recorded elapsed days since contract signing fall short of the jurisdiction's cooling-off minimum -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "client-3")
          res (exec-op actor "t5" {:op :actuation/finalize-referral :subject "client-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:cooling-off-period-not-elapsed} (-> (store/ledger db) last :basis)))
      (is (empty? (store/referral-history db))))))

(deftest background-check-not-cleared-is-held-and-unoverridable
  (testing "an uncleared background check on a client -> HOLD, and never reaches request-approval -- exercised via :background-check/screen DIRECTLY, not via the actuation op against an unscreened client (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's, advertising's, polling's, research's, design's, nursing's, sports's, alliedhealth's, laundry's, holdco's and photo's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :background-check/screen :subject "client-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:background-check-not-cleared} (-> (store/ledger db) first :basis)))
      (is (nil? (store/background-check-of db "client-4")) "no clearance written"))))

(deftest finalize-referral-always-escalates-then-human-decides
  (testing "a clean, fully-assessed client still ALWAYS interrupts for human approval -- actuation/finalize-referral is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "client-1")
          r1 (exec-op actor "t7" {:op :actuation/finalize-referral :subject "client-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, referral-finalization record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:referral-finalized? (store/client db "client-1"))))
          (is (= 1 (count (store/referral-history db))) "one draft finalization record"))))))

(deftest double-finalization-is-held
  (testing "finalizing the same client's referral twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "client-1")
          _ (exec-op actor "t8a" {:op :actuation/finalize-referral :subject "client-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/finalize-referral :subject "client-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-finalized} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/referral-history db))) "still only the one earlier finalization"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :client/intake :subject "client-1"
                          :patch {:id "client-1" :client-name "Sato Kenji"}} operator)
      (exec-op actor "b" {:op :serviceplan/verify :subject "client-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
