# cloud-itonami-isic-9609

Open Business Blueprint for **ISIC Rev.5 9609**: Other personal
service activities n.e.c..

This repository publishes a personal-service-provider actor -- client
intake, per-jurisdiction consumer-protection/cooling-off regulatory
assessment, background-check screening and personal-referral/
introduction finalization -- as an OSS business that any qualified,
licensed operator can fork, deploy, run, improve and sell, so a
community or independent professional never surrenders customer data
and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710),
[`8541`](https://github.com/cloud-itonami/cloud-itonami-isic-8541),
[`8690`](https://github.com/cloud-itonami/cloud-itonami-isic-8690),
[`9601`](https://github.com/cloud-itonami/cloud-itonami-isic-9601),
[`6420`](https://github.com/cloud-itonami/cloud-itonami-isic-6420),
[`7420`](https://github.com/cloud-itonami/cloud-itonami-isic-7420)) --
here it is **PersonalServiceOps-LLM ⊣ Personal Service Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a client-
> intake summary, normalizing records, and checking whether a client's
> own recorded elapsed days since contract signing arithmetically
> satisfy a jurisdiction's cooling-off period -- but it has **no
> notion of which jurisdiction's consumer-protection law is official,
> no license to finalize a real personal referral or introduction, and
> no way to know on its own whether a background check has actually
> stayed cleared**. Letting it finalize a referral directly invites
> fabricated regulatory citations, a referral being finalized before
> the client's own cooling-off period has actually elapsed, and an
> uncleared background check being quietly overlooked -- and
> liability, and consumer-protection risk, for whoever runs it. This
> project seals the PersonalServiceOps-LLM into a single node and
> wraps it with an independent **Personal Service Governor**, a human
> **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers client intake through consumer-protection/cooling-
off regulatory assessment, background-check screening and personal-
referral/introduction finalization. It does **not**, by itself, hold
any license required to operate as a personal-service provider in a
given jurisdiction, and it does not claim to. It also does not perform
the actual matchmaking/genealogical-research/concierge work itself,
or judge its quality -- `personalservice.registry/cooling-off-period-
not-elapsed?` is a pure temporal recompute against the client's own
recorded fields, not a service-quality review. Whoever deploys and
operates a live instance (a licensed personal-service provider)
supplies any jurisdiction-specific license, the real service delivery
and the real client-management/background-screening integrations, and
bears that jurisdiction's liability -- the software supplies the
governed, spec-cited, audited execution scaffold so that provider does
not have to build the compliance layer from scratch.

### Actuation

**Finalizing a real personal referral or introduction is never
autonomous, at any phase, by construction.** Two independent layers
enforce this (`personalservice.governor`'s `:actuation/finalize-
referral` high-stakes gate and `personalservice.phase`'s phase table,
which never puts `:actuation/finalize-referral` in any phase's `:auto`
set) -- see `personalservice.phase`'s docstring and `test/
personalservice/phase_test.clj`'s `finalize-referral-never-auto-at-
any-phase`. The actor may draft, check and recommend; a human provider
staff member is always the one who actually finalizes a referral.
Matching `leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/
`veterinary`'s/`funeral`'s/`parksafety`'s/`salon`'s/`entertainment`'s/
`facility`'s/`consulting`'s/`advertising`'s/`polling`'s/`research`'s/
`design`'s/`sports`'s/`alliedhealth`'s/`photo`'s single-actuation
shape, grounded directly in this blueprint's own README text ("No
automated proposal, by itself, can complete the following without
governor approval and audit evidence: finalizing a personal referral
or introduction") -- a POSITIVE actuation (finalizing a real record),
matching this fleet's majority actuation shape (`3600`/`6190` are the
fleet's two NEGATIVE-actuation exceptions).

## The core contract

```
client intake + jurisdiction facts (personalservice.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ PersonalServiceOps-LLM│ ─────────────▶ │ Personal Service              │  (independent system)
   │ (sealed)              │  + citations    │ Governor:                    │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · cooling-off-period-  │
          │                         │ not-elapsed (temporal ground-     │
    record + ledger        escalate ┼ truth recompute) · background-      │
          │              (ALWAYS for│ check-not-cleared (unconditional)  │
          │               :actuation│ · already-finalized                  │
          │               /finalize-│                                        │
          ▼               referral) └───────────────────────┘
      human approval
```

**The PersonalServiceOps-LLM never finalizes a referral the Personal
Service Governor would reject, and never does so without a human
sign-off.** Hard violations (fabricated regulatory requirements;
unsupported evidence; an un-elapsed cooling-off period; an uncleared
background check; a double finalization) force **hold** and *cannot*
be approved past; a clean finalization proposal still always routes
to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a document-courier robot
assists physical record handoff where used, under the actor, gated by
the independent **Personal Service Governor**. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions require
human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Personal Service Governor, referral-finalization draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`9609`). This vertical's client records are practice-specific rather
than a shared cross-operator data contract, so `personalservice.*` runs
on the generic robotics/identity/forms/dmn/bpmn/audit-ledger stack
only -- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/personalservice/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + referral-finalization history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded client, and the double-actuation guard checks a dedicated `:referral-finalized?` boolean rather than a `:status` value |
| `src/personalservice/registry.cljc` | Referral-finalization draft records, plus `cooling-off-period-not-elapsed?` -- the EIGHTH instance of this fleet's MINIMUM-threshold sufficiency check family (`veterinary`/`funeral`/`hospital` established the first three "temporal" instances, `association`/`secondary`/`polling`/`research` the fourth through seventh "non-temporal" instances), RETURNING to a temporal ground truth for a genuinely new domain concept |
| `src/personalservice/facts.cljc` | Per-jurisdiction consumer-protection/cooling-off catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/personalservice/personalserviceadvisor.cljc` | **PersonalServiceOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/service-plan-verification/background-check-screening/referral-finalization proposals |
| `src/personalservice/governor.cljc` | **Personal Service Governor** -- 4 HARD checks (spec-basis · evidence-incomplete · cooling-off-period-not-elapsed, temporal ground-truth recompute, GENUINELY NEW · background-check-not-cleared, unconditional evaluation, the THIRD literal instance of `school`'s/`sports`'s concept, the 48th grounding of this discipline overall) + already-finalized guard + 1 soft (confidence/actuation gate) |
| `src/personalservice/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (referral finalization always human; client intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/personalservice/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/personalservice/sim.cljc` | demo driver |
| `test/personalservice/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers client intake through consumer-protection/cooling-
off regulatory assessment, background-check screening and personal-
referral/introduction finalization -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Client intake + per-jurisdiction cooling-off/evidence checklisting, HARD-gated on an official spec-basis citation (`:client/intake`/`:serviceplan/verify`) | Real client-management/background-screening integration, real matchmaking/genealogical-research/concierge work itself (see `personalservice.facts`'s docstring) |
| Background-check screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:background-check/screen`) | Any service-quality judgment itself -- deliberately outside this actor's competence |
| Referral finalization, HARD-gated on full evidence, the client's own elapsed cooling-off period and a cleared background check, plus a double-finalization guard (`:actuation/finalize-referral`) | |
| Immutable audit ledger for every intake/verification/screening/finalization decision | |

Extending coverage is additive: add the next gate (e.g. a fee-
disclosure-acknowledgment check) as its own governed op with its own
HARD checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world
act" pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`personalservice.facts/coverage` reports how many requested
jurisdictions actually have an official spec-basis in `personalservice.
facts/catalog` -- currently 4 seeded (JPN, USA, GBR, DEU) out of ~194
jurisdictions worldwide. This is a starting catalog to prove the
governor contract end-to-end, not a claim of global coverage. Adding a
jurisdiction is additive: one map entry in `personalservice.facts/
catalog`, citing a real official source -- never fabricate a
jurisdiction's requirements to make coverage look bigger.

## Maturity

`:implemented` -- `PersonalServiceOps-LLM` + `Personal Service
Governor` run as real, tested code (see `Run` above), promoted from
the originally-published `:blueprint`-tier scaffold, modeled closely
on the sixty-three prior actors' architecture. See `docs/adr/0001-
architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
