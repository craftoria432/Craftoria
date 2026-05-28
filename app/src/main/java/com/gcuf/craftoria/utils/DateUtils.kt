package com.gcuf.craftoria.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(timestamp))
