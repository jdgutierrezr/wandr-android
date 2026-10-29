package com.kotlin.wandr.data.mapper

import java.time.Instant
import java.time.OffsetDateTime

internal fun String.toInstant(): Instant = OffsetDateTime.parse(this).toInstant()

internal fun String.toEpochMillis(): Long = toInstant().toEpochMilli()

internal fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
