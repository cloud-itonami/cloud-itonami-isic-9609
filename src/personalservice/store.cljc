(ns personalservice.store
  "SSoT for the personal-service actor, behind a `Store` protocol so
  the backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/personalservice/store_contract_test.clj), which is the whole
  point: the actor, the Personal Service Governor and the audit
  ledger never know which SSoT they run on.

  Like `clinic.store`'s/`credit.store`'s/`accounting.store`'s simpler
  entities, a CLIENT is acted on directly by the ONE actuation op --
  no dynamically-filed sub-record, and the double-finalization guard
  checks a dedicated `:referral-finalized?` boolean rather than a
  `:status` value, the same discipline `clinic.governor`'s/
  `accounting.governor`'s/`marketadmin.governor`'s guards establish.

  NOTE on naming: the protocol's per-entity accessor is `client`
  directly -- not a Clojure special form, so no `-of` suffix
  workaround was needed.

  The ledger stays append-only on every backend: 'which client was
  screened for a cleared background check, which referral was
  finalized, on what jurisdictional basis, approved by whom' is
  always a query over an immutable log -- the audit trail a customer
  trusting a personal-service provider needs, and the evidence an
  operator needs if a referral decision is later disputed."
  (:require [personalservice.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (client [s id])
  (all-clients [s])
  (background-check-of [s client-id] "committed background-check screening verdict for a client, or nil")
  (serviceplan-of [s client-id] "committed service-plan evidence assessment, or nil")
  (ledger [s])
  (referral-history [s] "the append-only referral-finalization history (personalservice.registry drafts)")
  (next-sequence [s jurisdiction] "next referral-number sequence for a jurisdiction")
  (client-already-finalized? [s client-id] "has this client's referral already been finalized?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-clients [s clients] "replace/seed the client directory (map id->client)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained client set so the actor + tests run
  offline."
  []
  {:clients
   {"client-1" {:id "client-1" :client-name "Sato Kenji"
               :days-since-contract-signed 5
               :background-check-not-cleared? false
               :referral-finalized? false :jurisdiction "JPN" :status :intake}
    "client-2" {:id "client-2" :client-name "Atlantis Doe"
               :days-since-contract-signed 5
               :background-check-not-cleared? false
               :referral-finalized? false :jurisdiction "ATL" :status :intake}
    "client-3" {:id "client-3" :client-name "鈴木花子"
               :days-since-contract-signed 1
               :background-check-not-cleared? false
               :referral-finalized? false :jurisdiction "JPN" :status :intake}
    "client-4" {:id "client-4" :client-name "田中一郎"
               :days-since-contract-signed 5
               :background-check-not-cleared? true
               :referral-finalized? false :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-referral!
  "Backend-agnostic `:client/mark-finalized` -- looks up the client via
  the protocol and drafts the referral-finalization record, and
  returns {:result .. :client-patch ..} for the caller to persist."
  [s client-id]
  (let [c (client s client-id)
        seq-n (next-sequence s (:jurisdiction c))
        result (registry/register-referral-finalization client-id (:jurisdiction c) seq-n)]
    {:result result
     :client-patch {:referral-finalized? true
                    :referral-number (get result "referral_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (client [_ id] (get-in @a [:clients id]))
  (all-clients [_] (sort-by :id (vals (:clients @a))))
  (background-check-of [_ id] (get-in @a [:background-checks id]))
  (serviceplan-of [_ client-id] (get-in @a [:serviceplans client-id]))
  (ledger [_] (:ledger @a))
  (referral-history [_] (:referrals @a))
  (next-sequence [_ jurisdiction] (get-in @a [:sequences jurisdiction] 0))
  (client-already-finalized? [_ client-id] (boolean (get-in @a [:clients client-id :referral-finalized?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :client/upsert
      (swap! a update-in [:clients (:id value)] merge value)

      :serviceplan/set
      (swap! a assoc-in [:serviceplans (first path)] payload)

      :background-check/set
      (swap! a assoc-in [:background-checks (first path)] payload)

      :client/mark-finalized
      (let [client-id (first path)
            {:keys [result client-patch]} (finalize-referral! s client-id)
            jurisdiction (:jurisdiction (client s client-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:sequences jurisdiction] (fnil inc 0))
                       (update-in [:clients client-id] merge client-patch)
                       (update :referrals registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-clients [s clients] (when (seq clients) (swap! a assoc :clients clients)) s))

(defn seed-db
  "A MemStore seeded with the demo client set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :serviceplans {} :background-checks {} :ledger [] :sequences {}
                           :referrals []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (serviceplan/background-check payloads, ledger
  facts, referral records) are stored as EDN strings so `langchain.db`
  doesn't expand them into sub-entities -- the same convention every
  sibling actor's store uses."
  {:client/id                   {:db/unique :db.unique/identity}
   :serviceplan/client-id        {:db/unique :db.unique/identity}
   :background-check/client-id    {:db/unique :db.unique/identity}
   :ledger/seq                     {:db/unique :db.unique/identity}
   :referral/seq                    {:db/unique :db.unique/identity}
   :sequence/jurisdiction             {:db/unique :db.unique/identity}})

;; the EDN-blob codec (enc/dec*) is shared machinery -- see
;; kotoba-lang/langchain-store's docstring (ADR-2607141600).
(defn- enc [v] (ls/enc v))
(defn- dec* [s] (ls/dec* s))

(defn- client->tx [{:keys [id client-name days-since-contract-signed
                          background-check-not-cleared? referral-finalized?
                          jurisdiction status referral-number]}]
  (cond-> {:client/id id}
    client-name                             (assoc :client/client-name client-name)
    days-since-contract-signed               (assoc :client/days-since-contract-signed days-since-contract-signed)
    (some? background-check-not-cleared?)     (assoc :client/background-check-not-cleared? background-check-not-cleared?)
    (some? referral-finalized?)                (assoc :client/referral-finalized? referral-finalized?)
    jurisdiction                                 (assoc :client/jurisdiction jurisdiction)
    status                                        (assoc :client/status status)
    referral-number                                (assoc :client/referral-number referral-number)))

(def ^:private client-pull
  [:client/id :client/client-name :client/days-since-contract-signed
   :client/background-check-not-cleared? :client/referral-finalized?
   :client/jurisdiction :client/status :client/referral-number])

(defn- pull->client [m]
  (when (:client/id m)
    {:id (:client/id m) :client-name (:client/client-name m)
     :days-since-contract-signed (:client/days-since-contract-signed m)
     :background-check-not-cleared? (boolean (:client/background-check-not-cleared? m))
     :referral-finalized? (boolean (:client/referral-finalized? m))
     :jurisdiction (:client/jurisdiction m) :status (:client/status m)
     :referral-number (:client/referral-number m)}))

(defrecord DatomicStore [conn]
  Store
  (client [_ id]
    (pull->client (d/pull (d/db conn) client-pull [:client/id id])))
  (all-clients [_]
    (->> (d/q '[:find [?id ...] :where [?e :client/id ?id]] (d/db conn))
         (map #(pull->client (d/pull (d/db conn) client-pull [:client/id %])))
         (sort-by :id)))
  (background-check-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?cid
                :where [?k :background-check/client-id ?cid] [?k :background-check/payload ?p]]
              (d/db conn) id)))
  (serviceplan-of [_ client-id]
    (dec* (d/q '[:find ?p . :in $ ?cid
                :where [?a :serviceplan/client-id ?cid] [?a :serviceplan/payload ?p]]
              (d/db conn) client-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (referral-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :referral/seq ?s] [?e :referral/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :sequence/jurisdiction ?j] [?e :sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (client-already-finalized? [s client-id]
    (boolean (:referral-finalized? (client s client-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :client/upsert
      (d/transact! conn [(client->tx value)])

      :serviceplan/set
      (d/transact! conn [{:serviceplan/client-id (first path) :serviceplan/payload (enc payload)}])

      :background-check/set
      (d/transact! conn [{:background-check/client-id (first path) :background-check/payload (enc payload)}])

      :client/mark-finalized
      (let [client-id (first path)
            {:keys [result client-patch]} (finalize-referral! s client-id)
            jurisdiction (:jurisdiction (client s client-id))
            next-n (inc (next-sequence s jurisdiction))]
        (d/transact! conn
                     [(client->tx (assoc client-patch :id client-id))
                      {:sequence/jurisdiction jurisdiction :sequence/next next-n}
                      {:referral/seq (count (referral-history s)) :referral/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-clients [s clients]
    (when (seq clients) (d/transact! conn (mapv client->tx (vals clients)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:clients ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [clients]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-clients s clients))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo client set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
