package com.xcloak.airflux.core.common

import android.os.Environment
import android.os.StatFs

object StorageUtils {
    /** Returns available bytes on the primary external storage. */
    fun availableBytes(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        return stat.availableBytes
    }

    fun hasEnoughSpace(requiredBytes: Long, bufferBytes: Long = 50L * 1024 * 1024): Boolean {
        if (requiredBytes <= 0) return true // unknown size — can't pre-check, proceed and let it fail naturally if truly out of space
        return availableBytes() > requiredBytes + bufferBytes
    }
}