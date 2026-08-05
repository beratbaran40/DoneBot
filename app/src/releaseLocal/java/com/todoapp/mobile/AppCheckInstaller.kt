package com.todoapp.mobile

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * The debug provider, not Play Integrity — deliberately.
 *
 * `releaseLocal` is release code installed by hand, so Play Integrity has nothing to attest: the
 * build was never distributed by Play and every App Check call would be rejected, taking the whole
 * backend session down with it. The debug provider keeps the app usable while everything that
 * `releaseLocal` exists to test (R8, resource shrinking, the baseline profile) stays identical to
 * release.
 *
 * `installAppCheck` has one declaration per source set, so a build type without its own file simply
 * fails to compile — which is why this exists at all.
 */
fun installAppCheck() {
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance(),
    )
}
