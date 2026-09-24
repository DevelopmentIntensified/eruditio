# One-time Play Billing product; no subscriptions

The core unlock (and any future content packs) sells as a one-time Play Billing product. Subscriptions would charge monthly for friction that already benefits the user directly — the anti-pattern Eruditio exists to escape — and a recurring relationship would pull the product toward engagement metrics that the spec explicitly demotes. Ads and attribution SDKs are excluded for the same reason.

Status: accepted

## Consequences

- Future content tracks arrive as optional one-time packs, not a catalog subscription.
- Debug builds bypass the billing gate entirely; the gate is `TrialGate` (free-session allowance) plus one-time ownership.
