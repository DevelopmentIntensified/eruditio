# 10: One-time billing

**What to build:** The "buy once, never asked monthly" promise: Google Play Billing with a one-time product that unlocks the core app. A fresh install can evaluate the app before purchase; the purchase persists across reinstalls via Play's licensing. No subscriptions, no ads, no third-party billing SDKs. Debug builds bypass the gate so development stays friction-free.

**Blocked by:** 07 (Completion feeds the engine).

**Status:** ready-for-human

- [ ] Fresh install gates the core loop until purchase
- [ ] One-time purchase completes in Play internal testing and unlocks the app
- [ ] Owned state survives reinstall (Play licensing check)
- [ ] Debug builds bypass the gate
- [ ] No subscription or ad SDK appears anywhere in the build

**2026-09-24 — agent**: Play Billing 8.3 wired: one-time product `eruditio_core_unlock`, acknowledge + restore (Play licensing), pending purchases enabled via enableOneTimeProducts (verified crash without it on device). Client lazy so debug builds never initialize billing; gate bypassed in debug at the seam. Home card unlocks reactively from the Room entitlement flow; purchase errors surface on the card. Remaining human steps: Play internal testing, purchase + reinstall survival check.
