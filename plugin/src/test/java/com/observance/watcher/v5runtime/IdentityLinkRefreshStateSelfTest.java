package com.observance.watcher.v5runtime;

import java.util.UUID;

/** Focused regression test for post-join link recognition, recovery revocation, and outage safety. */
public final class IdentityLinkRefreshStateSelfTest {
    private IdentityLinkRefreshStateSelfTest() {}

    public static void main(String[] args) {
        IdentityLinkRefreshState state = new IdentityLinkRefreshState();
        UUID actor = UUID.fromString("6ca7cb62-344b-42de-af3e-57e90c19223b");

        check(!state.begin(actor, false), "unvalidated authority must not start a DB lookup");
        check(state.begin(actor, true), "validated authority starts the first lookup");
        check(state.inFlight(actor), "lookup is marked in flight");
        check(!state.begin(actor, true), "concurrent polls deduplicate one player's lookup");

        state.finish(actor, IdentityLinkRefreshState.Observation.INDETERMINATE);
        check(!state.linked(actor), "an indeterminate first read is not cached as linked");
        check(!state.inFlight(actor), "a failed result releases the retry latch");
        check(state.begin(actor, true), "the next online poll can retry without reconnect");

        state.finish(actor, IdentityLinkRefreshState.Observation.LINKED);
        check(state.linked(actor), "a positive Discord link becomes visible");
        check(state.snapshot().contains(actor), "linked-player snapshots include the new link");
        check(state.begin(actor, true), "a linked hand remains eligible for periodic revalidation");

        state.finish(actor, IdentityLinkRefreshState.Observation.INDETERMINATE);
        check(state.linked(actor), "an outage preserves the last authoritative linked result");
        check(state.begin(actor, true), "the online poll continues after an outage");

        state.finish(actor, IdentityLinkRefreshState.Observation.UNLINKED);
        check(!state.linked(actor), "an authoritative blank discord_id revokes the recovered old hand");
        check(!state.snapshot().contains(actor), "revoked hands leave linked-player snapshots");
        check(state.begin(actor, true), "a revoked hand can be observed linking again later");

        state.finish(actor, IdentityLinkRefreshState.Observation.LINKED);
        check(state.linked(actor), "a later authoritative relink is accepted without reconnect");

        state.finish(null, IdentityLinkRefreshState.Observation.LINKED);
        check(!state.linked(null), "null identities remain safely unlinked");
        System.out.println("Identity link refresh selftest OK: retry + outage preservation + recovery revocation");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
