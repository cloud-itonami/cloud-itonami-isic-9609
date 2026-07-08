# ADR-0001: PersonalServiceOps-LLM ⊣ Personal Service Governor architecture

## Status

Accepted. `cloud-itonami-isic-9609` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-9609` publishes an OSS business blueprint for
other personal service activities n.e.c.: matchmaking services,
genealogical research, personal shopping/concierge and similar
personal-referral services. Like every prior actor in this fleet, the
blueprint alone is not an implementation: this ADR records the
governed-actor architecture that promotes it to real, tested code,
following the same langgraph StateGraph + independent Governor +
Phase 0→3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across sixty-three prior siblings, most
recently `cloud-itonami-isic-7420` (photographic activities).

## Decision

### Decision 1: single-actuation shape

This blueprint's business-model.md Trust Controls mention both
"a service is performed" and "a personal referral is finalized" as
needing human sign-off, while the README's own "No automated
proposal... can complete the following without governor approval and
audit evidence:" phrasing names only ONE act ("finalizing a personal
referral or introduction"). Following `sports`/8541's precedent for
this same either/or-naming ambiguity (its "certification or safety-
relevant progress record" phrasing), this build treats the either/or
naming as ONE conceptual act. Matching `leasing`/`underwriting`/
`testlab`/`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/
`entertainment`/`facility`/`consulting`/`advertising`/`polling`/
`research`/`design`/`sports`/`alliedhealth`/`photo`'s single-
actuation shape, `high-stakes` here is a one-member set,
`#{:actuation/finalize-referral}`.

### Decision 2: entity and op shape

The primary entity is a `client`, matching the README's own Core
Contract language ("intake + identity + service/member records").
Four ops: `:client/intake` (directory upsert, no capital risk),
`:serviceplan/verify` (per-jurisdiction cooling-off/evidence
checklist, never auto), `:background-check/screen` (background-check
screening, unconditional-evaluation discipline, never auto), and
`:actuation/finalize-referral` (POSITIVE, high-stakes -- finalizing a
real personal referral or introduction).

### Decision 3: `cooling-off-period-not-elapsed?` -- the 8th MINIMUM-threshold sufficiency check, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for "cooling-off", "cancellation-period" and
"rescission" -- zero hits, confirming this is a genuinely new domain
concept, avoiding the false-precedent-claim risk `leasing`'s ADR-0001
documents. `personalservice.registry/cooling-off-period-not-elapsed?`
recomputes `(< days-since-contract-signed minimum-cooling-off-period-
days)` directly from the client's own recorded field -- the EIGHTH
instance of this fleet's MINIMUM-threshold sufficiency check family
(`veterinary`/`funeral`/`hospital` established the first three,
temporal; `association`/`secondary`/`polling`/`research` generalized
it to non-temporal ground truths as the fourth through seventh),
RETURNING to a temporal ground truth here: California's Dating
Service Contracts Act §1694 (3-business-day minimum), Japan's Act on
Specified Commercial Transactions cooling-off provisions, and the
EU/UK 14-day consumer-contract cancellation right. Gates only
`:actuation/finalize-referral`.

### Decision 4: `background-check-not-cleared-violations` -- an honest THIRD literal reuse, not claimed as new

`school.governor` established this concept FIRST;
`sports.governor/background-check-not-cleared-violations` reused it
literally as the SECOND instance. `personalservice.governor/
background-check-not-cleared-violations` is the THIRD literal
instance of this specific concept, and the 48th distinct application
of the unconditional-evaluation discipline overall (`casualty.
governor/sanctions-violations`'s original fix, most recently
`photo.governor/minor-subject-guardian-consent-unresolved-
violations` at 47th) -- not claimed as new. Evaluated unconditionally
so `:background-check/screen` can HARD-hold on its own finding, not
only when reached via the actuation op. Gates `:background-check/
screen` and `:actuation/finalize-referral`.

### Decision 5: dedicated double-actuation-guard boolean

`:referral-finalized?` is a dedicated boolean on the `client` record,
never a single `:status` value -- the same discipline every prior
sibling governor's guards establish, informed by `cloud-itonami-
isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`personalservice.store/Store` is implemented by both `MemStore`
(atom-backed, default for dev/tests/demo) and `DatomicStore`
(`langchain.db`-backed), proven to satisfy the same contract in
`test/personalservice/store_contract_test.clj` -- the same seam
every sibling actor uses so swapping the SSoT backend is a
configuration change, not a rewrite. The protocol's per-entity
accessor is named `client` directly -- not a Clojure special form, so
no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:client/intake` (no
capital risk). `:serviceplan/verify` and `:background-check/screen`
are never auto-eligible at any phase (matching every sibling's
screening-op posture), and `:actuation/finalize-referral` is
permanently excluded from every phase's `:auto` set -- a structural
fact, not a rollout milestone, enforced by BOTH `personalservice.
phase` and `personalservice.governor`'s `high-stakes` set
independently.

### Decision 8: no bespoke domain capability lib

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`personalservice.personalserviceadvisor` provides `mock-advisor`
(deterministic, default everywhere -- the actor graph and governor
contract run offline) and `llm-advisor` (backed by `langchain.model/
ChatModel`, with a defensive EDN-proposal parser so a malformed LLM
response degrades to a safe low-confidence noop rather than ever
auto-finalizing a referral).

### Decision 10: current `deps.edn` coordinates from the start

Following `holdco`/6420's discovery that `com-junkawasaki/langgraph-
clj`/`langchain-clj` were transferred and renamed upstream to
`kotoba-lang/langgraph`/`langchain` (internal namespaces unchanged --
only the artifact coordinate and `:local/root` path), this repo's
`deps.edn` used the CORRECTED `io.github.kotoba-lang/langgraph` +
`io.github.kotoba-lang/langchain` coordinates from its very first
draft, avoiding the bug proactively (same as `photo`/7420).

## Alternatives considered

- **A dual-actuation shape** splitting "service performed" and
  "referral finalized" into two separate high-stakes acts. Rejected:
  the README's own "No automated proposal... can complete the
  following" phrasing names only one act; the business-model.md
  Trust Controls' either/or phrasing is treated as one conceptual act,
  following `sports`/8541's precedent for the same ambiguity.
- **Folding the cooling-off check into the generic evidence-
  completeness checklist alone**, without a dedicated ground-truth
  recompute. Rejected: consumer-protection cooling-off law is a
  temporal, jurisdiction-specific minimum independent of whatever
  evidence checklist a serviceplan proposal cites -- a dedicated,
  unconditionally-recomputed check more precisely matches real
  cooling-off law than folding it into the evidence checklist.
- **Treating the background-check reuse as a "renamed for domain
  fit" honest reuse** (like `holdco`'s `certification-not-current`).
  Rejected: the concept, field name and check shape are IDENTICAL to
  `school`'s/`sports`'s -- a literal reuse, not a rename, is the
  honest characterization.

## Consequences

- Sixty-fourth actor in this fleet (63 implemented before this
  build).
- Confirms the MINIMUM-threshold sufficiency check family generalizes
  to an 8th instance, returning to a temporal ground truth for a
  genuinely new domain concept (cooling-off periods).
- Documents an honest THIRD literal reuse of the background-check-
  not-cleared concept, correcting an initial docstring draft that
  undercounted the reuse ordinal.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/
  personalservice/store_contract_test.clj`, the same `:db-api`-driven
  swap pattern every sibling actor uses.
- `blueprint.edn` required only the `:maturity` field addition -- `:id`,
  `:required-technologies` and `:optional-technologies` already
  matched the `kotoba-lang/industry` registry's own entry for
  `"9609"` exactly.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-9609/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-9609/docs/business-model.md`
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"9609"`)
