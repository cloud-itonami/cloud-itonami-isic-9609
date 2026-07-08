# Business Model: Other personal service activities n.e.c.

## Classification

- Repository: `cloud-itonami-isic-9609`
- ISIC Rev.5: `9609`
- Activity: other personal service activities not elsewhere classified (e.g. matchmaking services, genealogical research, personal shopping/concierge)
- Social impact: community access, data sovereignty, transparent audit

## Customer

- independent personal-service providers
- cooperative service collectives
- community personal-service programs

## Offer

- client booking intake
- service-plan proposal
- service-completion proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per provider
- support: monthly retainer with SLA
- migration: import from an incumbent service-booking system
- per-service fee

## Trust Controls

- no service is performed and no personal referral is finalized without human sign-off
- a fabricated background/verification check forces a hold, not an override
- every service path is auditable
- client personal data stays outside Git
- emergency manual override paths remain outside LLM control
- a client whose own recorded elapsed days since contract signing fall
  short of the jurisdiction's cooling-off period, or an uncleared
  background check, forces a hold, not an override
- referral finalization is logged and escalated, and cannot be
  finalized twice for the same client: a double-finalization attempt
  is held off this actor's own client facts alone, with no upstream
  comparison needed

## Personal Service Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:personal-
service-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (finalizing a
real personal referral or introduction) must pass. The governor sits
between the PersonalServiceOps-LLM and execution, per the README's
Core Contract:

```text
PersonalServiceOps-LLM -> Personal Service Governor -> hold, proceed, or human approval
```

**Approves**: routine personal-service actions proposed against a
client that already has a consented service plan on file, satisfied
required evidence, an elapsed cooling-off period, and a cleared
background check. These proceed straight to the client ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize a referral on its own authority when any of the following
hold -- a fabricated jurisdiction spec-basis; incomplete evidence; an
un-elapsed cooling-off period; an uncleared background check; a
double-finalization attempt. A clean finalization proposal still
always routes to a human -- `:actuation/finalize-referral` is never
auto-committed, at any rollout phase.
