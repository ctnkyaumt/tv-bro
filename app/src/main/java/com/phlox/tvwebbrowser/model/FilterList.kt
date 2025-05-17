package com.phlox.tvwebbrowser.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Represents an adblock filter list
 */
@Parcelize
data class FilterList(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    var enabled: Boolean = true,
    var lastUpdated: Long = 0
) : Parcelable
