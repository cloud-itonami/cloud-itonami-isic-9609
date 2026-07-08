(ns personalservice.governor
  "Personal Service Governor -- the independent compliance layer that
  earns the PersonalServiceOps-LLM the right to commit. The LLM has no
  notion of jurisdictional consumer-cancellation/personal-referral
  law, whether a client's own recorded elapsed days since contract
  signing actually satisfy the jurisdiction's own recorded minimum
  cooling-off period, whether a background check has actually stayed
  cleared, or when an act stops being a draft and becomes a real-world
  referral finalization, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD -- the personal-service
  analog of `cloud-itonami-isic-8620`'s ClinicGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, an
  un-elapsed cooling-off period, an uncleared background check, or a
  double finalization). The confidence/actuation gate is SOFT: it
  asks a human to look (low confidence / actuation), and the human may
  approve -- but see `personalservice.phase`: for `:stake :actuation/
  finalize-referral` (a real referral/introduction finalization) NO
  phase ever allows auto-commit either. Two independent layers agree
  that actuation is always a human call.

    1. Spec-basis                  -- did the service-plan proposal
                                       cite an OFFICIAL source
                                       (`personalservice.facts`), or
                                       invent one?
    2. Evidence incomplete         -- for `:actuation/finalize-
                                       referral`, has the client
                                       actually been assessed with a
                                       full client-consent-record/
                                       service-plan-record/
                                       background-check-verification-
                                       record/referral-completion-
                                       record evidence checklist on
                                       file?
    3. Cooling-off period not
       elapsed                        -- for `:actuation/finalize-
                                       referral`, INDEPENDENTLY
                                       recompute whether the client's
                                       own recorded elapsed days since
                                       contract signing fall short of
                                       the jurisdiction's own required
                                       minimum cooling-off period
                                       (`personalservice.registry/
                                       cooling-off-period-not-
                                       elapsed?`) -- needs no proposal
                                       inspection at all. A GENUINELY
                                       NEW concept in this fleet,
                                       grep-verified absent from every
                                       prior sibling's check names --
                                       the EIGHTH instance of this
                                       fleet's MINIMUM-threshold
                                       sufficiency check family
                                       (`veterinary`/`funeral`/
                                       `hospital` established the
                                       first three, temporal;
                                       `association`/`secondary`/
                                       `polling`/`research` generalized
                                       it to non-temporal ground truths
                                       as the fourth through seventh),
                                       returning to a temporal ground
                                       truth for a genuinely new
                                       domain concept.
    4. Background check not
       cleared                        -- reported by THIS proposal
                                       itself (a `:background-check/
                                       screen` that just found an
                                       uncleared check), or already on
                                       file for the client
                                       (`:background-check/screen`/
                                       `:actuation/finalize-
                                       referral`). Evaluated
                                       UNCONDITIONALLY (not scoped to
                                       a specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...(forty-
                                       seven prior siblings, most
                                       recently `photo.governor/minor-
                                       subject-guardian-consent-
                                       unresolved-violations`)...
                                       established -- a LITERAL reuse
                                       of `school.governor/background-
                                       check-not-cleared-violations`'s
                                       own concept (the FIRST instance;
                                       `sports.governor` reused it
                                       literally as the SECOND) -- the
                                       THIRD literal instance of this
                                       specific concept, and the 48th
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall, not claimed
                                       as new.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-referral` (a REAL
                                       personal-referral act) ->
                                       escalate.

  One more guard, double-finalization prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-finalized-violations` refuses to
  finalize a referral for the SAME client twice, off a dedicated
  `:referral-finalized?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [personalservice.facts :as facts]
            [personalservice.registry :as registry]
            [personalservice.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real personal referral or introduction is the ONE
  real-world actuation event this actor performs -- a single-member
  set, matching `cloud-itonami-isic-6511`'s/`6621`'s/`6629`'s/`6612`'s/
  `6492`'s/`7120`'s/`8620`'s single-actuation shape."
  #{:actuation/finalize-referral})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:serviceplan/verify` (or `:actuation/finalize-referral`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's personal-referral-service requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:serviceplan/verify :actuation/finalize-referral} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はクーリング・オフ基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/finalize-referral`, the jurisdiction's required
  client-consent-record/service-plan-record/background-check-
  verification-record/referral-completion-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-referral)
    (let [c (store/client st subject)
          serviceplan (store/serviceplan-of st subject)]
      (when-not (and serviceplan
                     (facts/required-evidence-satisfied?
                      (:jurisdiction c) (:checklist serviceplan)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(顧客同意記録/サービス計画記録/身元確認記録/紹介完了記録等)が充足していない状態での提案"}]))))

(defn- cooling-off-period-not-elapsed-violations
  "For `:actuation/finalize-referral`, INDEPENDENTLY recompute whether
  the client's own recorded elapsed days since contract signing fall
  short of the required minimum cooling-off period via
  `personalservice.registry/cooling-off-period-not-elapsed?` -- needs
  no proposal inspection at all, since its inputs are permanent
  ground-truth fields already on the client."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-referral)
    (let [c (store/client st subject)]
      (when (registry/cooling-off-period-not-elapsed? c)
        [{:rule :cooling-off-period-not-elapsed
          :detail (str subject " の契約締結からの経過日数(" (:days-since-contract-signed c)
                      ")がクーリング・オフ期間(" registry/minimum-cooling-off-period-days ")日に不足")}]))))

(defn- background-check-not-cleared-violations
  "An uncleared background check -- reported by THIS proposal (e.g. a
  `:background-check/screen` that itself just found an uncleared
  check), or already on file in the store for the client
  (`:background-check/screen`/`:actuation/finalize-referral`) -- is a
  HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not scoped to
  a specific op) so the screening op itself can HARD-hold on its own
  finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :not-cleared (get-in proposal [:value :verdict]))
        client-id (when (contains? #{:background-check/screen :actuation/finalize-referral} op) subject)
        hit-on-file? (and client-id (= :not-cleared (:verdict (store/background-check-of st client-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :background-check-not-cleared
        :detail "身元確認が未完了の状態での紹介確定提案は進められない"}])))

(defn- already-finalized-violations
  "For `:actuation/finalize-referral`, refuses to finalize a referral
  for the SAME client twice, off a dedicated `:referral-finalized?`
  fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-referral)
    (when (store/client-already-finalized? st subject)
      [{:rule :already-finalized
        :detail (str subject " は既に紹介確定済み")}])))

(defn check
  "Censors a PersonalServiceOps-LLM proposal against the governor
  rules. Returns {:ok? bool :violations [..] :confidence c :escalate?
  bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (cooling-off-period-not-elapsed-violations request st)
                           (background-check-not-cleared-violations request proposal st)
                           (already-finalized-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
