package com.example.timecard.domain

/** Result of applying admin badge grants and revokes to a profile. */
data class GrantChangeResult(
    val badges: Map<String, Int>,
    val grantCoins: Map<String, Int>,
    val coinDelta: Int
)

/**
 * Applies admin grants ([newlyGranted]) and revokes ([revoked]) to [badges].
 *
 * A grant adds one of the badge and pays [grantAmount] (recorded in grantCoins). A revoke takes back
 * exactly what that grant paid and removes one of the badge. A revoke with no recorded payment
 * (a grant tracked before payments were recorded, or one for a badge the employee had already
 * earned themselves, which was never paid) only stops tracking it: it never removes a badge or
 * coins the grant didn't give.
 */
fun applyGrantChanges(
    badges: Map<String, Int>,
    grantCoins: Map<String, Int>,
    newlyGranted: List<String>,
    revoked: List<String>,
    grantAmount: (String) -> Int
): GrantChangeResult {
    val updatedBadges = badges.toMutableMap()
    val updatedGrantCoins = grantCoins.toMutableMap()
    var delta = 0
    for (id in newlyGranted) {
        updatedBadges[id] = (updatedBadges[id] ?: 0) + 1
        val amount = grantAmount(id)
        updatedGrantCoins[id] = amount
        delta += amount
    }
    for (id in revoked) {
        val paid = updatedGrantCoins.remove(id) ?: continue
        val count = updatedBadges[id] ?: 0
        if (count > 1) updatedBadges[id] = count - 1 else updatedBadges.remove(id)
        delta -= paid
    }
    return GrantChangeResult(updatedBadges, updatedGrantCoins, delta)
}
