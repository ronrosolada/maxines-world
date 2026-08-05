package com.maxinesworld.coredatabase

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/** Executes a block on Room's single transaction dispatcher. */
@Singleton
open class RoomTransactionRunner @Inject constructor(
    private val database: MaxinesDatabase,
) {
    open suspend fun <T> run(block: suspend () -> T): T = database.withTransaction(block)
}
