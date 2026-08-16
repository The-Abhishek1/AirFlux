package com.xcloak.airflux.core.common

import android.net.Uri

/** Bridges the Activity's onCreate/onNewIntent (share-from-another-app) to Compose,
 *  since the NavController isn't available yet when the Intent first arrives. */
object IncomingShareHolder {
    var pendingUris: List<Uri> = emptyList()

    fun consume(): List<Uri> {
        val list = pendingUris
        pendingUris = emptyList()
        return list
    }

    fun hasPending(): Boolean = pendingUris.isNotEmpty()
}