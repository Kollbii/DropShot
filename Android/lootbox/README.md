# Lootbox Module (`Android/lootbox/`)

This Android library provides the cosmetic squash lootbox screen, drop logic, reward inventory, and the AES-256-GCM encrypted storage engine (`SecureStorageEngine`).

---

## 1. Roll Logic and Timing

* `LootTier` lists tiers in rarest-first order with absolute percentage weights totaling 100 (`1%` Gold, `4%` Purple, `25%` Green, `70%` Gray).
* `TierRoller` evaluates tiers in rarest-first order with conditional probability `tier.weight / sum(remainingWeights)`.
* `LootboxAuthoritativeService` uses `SecureRandom` to roll rewards and issues single-use HMAC-SHA256 signed tickets bound to the active player profile ID.
* `RewardInventory` redeems signed tickets and persists unlocked reward IDs per profile through `SecureStorageEngine` (AES-256-GCM backed by AndroidKeyStore).
* During animation:
  1. The squash ball shakes for 1.0 s and cracks into 2 halves over 1.2 s.
  2. The horizontal reel travels fast for 1.5 s, slows over 3.0 s, and creeps for 2.0 s to a stop inside the winning square.
  3. If system animations are disabled (`isReduceMotionEnabled`), the result card displays immediately.

---

## 2. Build and Test

```bash
gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home :lootbox:testDebugUnitTest
```

See [AGENTS.md](AGENTS.md) and [REWARDS.md](REWARDS.md) for catalog and security rules.
