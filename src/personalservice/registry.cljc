(ns personalservice.registry
  "Pure-function referral-finalization record construction -- an
  append-only personal-service book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a referral-finalization
  reference number -- every provider/jurisdiction assigns its own
  reference format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `personalservice.facts` uses.

  `cooling-off-period-not-elapsed?` is a GENUINELY NEW check concept
  in this fleet (grep-verified absent from every prior sibling's
  check names before this claim was finalized -- no 'cooling-off'/
  'cancellation-period'/'rescission' concept exists anywhere else in
  this fleet). It reuses the MINIMUM-threshold sufficiency check
  SHAPE `veterinary.registry/withdrawal-period-insufficient?`/
  `funeral.registry/waiting-period-elapsed?`/`hospital.registry/
  observation-period-elapsed?` established (temporal, first three
  instances) and `association.registry/continuing-education-hours-
  insufficient?`/`secondary.registry/attendance-hours-insufficient?`/
  `polling.registry/sample-size-insufficient?`/`research.registry/
  replication-count-insufficient?` generalized (non-temporal, fourth
  through seventh instances) -- the EIGHTH instance overall, RETURNING
  to a temporal ground truth for a genuinely new domain concept: a
  client's own recorded elapsed days since contract signing must
  satisfy the jurisdiction's own recorded minimum cooling-off period
  before a real referral/introduction can be finalized, a direct,
  natural mapping onto real consumer-protection law (e.g. California's
  Dating Service Contracts Act §1694, Japan's Act on Specified
  Commercial Transactions cooling-off provisions for marriage-
  introduction services, the EU/UK 14-day consumer-contract
  cancellation right).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real service-booking system. It builds the RECORD a
  personal-service provider would keep, not the act of finalizing the
  referral itself (that is `personalservice.operation`'s `:actuation/
  finalize-referral`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  provider's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(def minimum-cooling-off-period-days
  "A jurisdiction-agnostic floor for this R0 scaffold, matching the
  shortest of the seeded jurisdictions' own official cooling-off
  requirements (California's 3-business-day minimum) -- a real
  deployment configures this per-jurisdiction from `personalservice.
  facts`, never a single global constant in production."
  3)

(defn cooling-off-period-not-elapsed?
  "Does `client`'s own recorded `:days-since-contract-signed` fall
  short of `minimum-cooling-off-period-days`? A pure ground-truth
  check against the client's own permanent field -- no upstream
  comparison needed. The EIGHTH instance of this fleet's MINIMUM-
  threshold sufficiency check family (see ns docstring)."
  [{:keys [days-since-contract-signed]}]
  (and (number? days-since-contract-signed)
       (< days-since-contract-signed minimum-cooling-off-period-days)))

(defn register-referral-finalization
  "Validate + construct the REFERRAL-FINALIZATION registration DRAFT
  -- the personal-service provider's own act of finalizing a real
  personal referral or introduction. Pure function -- does not touch
  any real service-booking system; it builds the RECORD a provider
  would keep. `personalservice.governor` independently re-verifies the
  client's own cooling-off-period ground truth and blocks a double-
  finalization for the same client, before this is ever allowed to
  commit."
  [client-id jurisdiction sequence]
  (when-not (and client-id (not= client-id ""))
    (throw (ex-info "referral-finalization: client_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "referral-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "referral-finalization: sequence must be >= 0" {})))
  (let [referral-number (str (str/upper-case jurisdiction) "-REF-" (zero-pad sequence 6))
        record {"record_id" referral-number
                "kind" "referral-finalization-draft"
                "client_id" client-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "referral_number" referral-number
     "certificate" (unsigned-certificate "ReferralFinalization" referral-number referral-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
