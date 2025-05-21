package com.gobot.receipt.db

import com.gobot.receipt.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ReceiptRepo {
    fun list(): List<ReceiptDTO> = transaction {
        Receipts.selectAll().map { it.toReceiptDto() }
    }

    fun find(id: UUID): ReceiptDTO? = transaction {
        Receipts.select { Receipts.id eq id }.singleOrNull()?.toReceiptDto()
    }

    fun itemsFor(id: UUID): List<ItemDTO> = transaction {
        Items.select { Items.receipt eq id }.map { it.toItemDto() }
    }
}
