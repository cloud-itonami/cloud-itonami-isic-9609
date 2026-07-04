# Governance

`cloud-itonami-isic-9609` is an OSS open-business blueprint for other personal service activities not elsewhere classified (e.g. matchmaking services, genealogical research, personal shopping/concierge).
Governance covers both the capability layer and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Personal Service Governor remains independent of the advisor.
- hard policy violations (fabricated verification, incomplete records) cannot be
  overridden by human approval.
- finalizing a personal referral or introduction always escalates to a human -- never automated.
- every hold, approval and action path is auditable.
- member/client/customer personal data stay outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit and data-flow review.

Certified operators can lose certification for:

- bypassing the Personal Service Governor's policy checks
- mishandling member/client/customer data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
