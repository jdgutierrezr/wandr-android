package com.kotlin.wandr.data.mapper

import java.time.Instant
import java.time.OffsetDateTime

/** Supabase sends timestamps like `2026-09-27T05:00:00.123456+00:00`. */
internal fun String.toInstant(): Instant = OffsetDateTime.parse(this).toInstant()

internal fun String.toEpochMillis(): Long = toInstant().toEpochMilli()

internal fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
