package com.gobot.receipt   // ваш пакет

import com.gobot.receipt.db.ReceiptRepo
import com.gobot.receipt.util.toUUID
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlinx.serialization.json.Json



fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}


fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureRouting()

    // 1. Запуск Flyway
    val cfg = environment.config
    Flyway.configure()
        .dataSource(cfg.property("db.url").getString(),
            cfg.property("db.user").getString(),
            cfg.property("db.pass").getString())
        .locations(cfg.property("flyway.locations").getString())
        .load()
        .migrate()

    // 2. Подключаем Exposed к той же базе
    Database.connect(
        cfg.property("db.url").getString(),
        driver = cfg.property("db.driver").getString(),
        user = cfg.property("db.user").getString(),
        password = cfg.property("db.pass").getString()
    )

    install(ContentNegotiation) { json() }

    install(Thymeleaf) {
        // Стандартный резолвер, читающий файлы из classpath:templates/
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"        // папка внутри resources
            suffix = ".html"
            characterEncoding = "UTF-8"
            // Опция live-reload в dev-режиме:
            cacheable = !environment.developmentMode
        })
    }

    /* Flyway + Database.connect() … */

    install(ContentNegotiation) { json() }
    install(Thymeleaf) { /* … */ }

    val repo = ReceiptRepo()      // ← создаём один экземпляр репозитория

    routing {

        /** REST-JSON, который мы добавили раньше */
        route("/receipts") { /* … */ }

        /** 👇 HTML-маршруты из шага E */
        route("/ui") {

            get("/receipts") {
                call.respond(
                    ThymeleafContent(
                        "receipts",
                        mapOf("receipts" to repo.list())
                    )
                )
            }

            get("/receipts/{id}") {
                val id = call.parameters["id"]!!.toUUID()
                val receipt = repo.find(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                val items = repo.itemsFor(id)
                call.respond(
                    ThymeleafContent(
                        "receipt-detail",
                        mapOf(
                            "receipt" to receipt,
                            "items"   to items
                        )
                    )
                )
            }
        }
    }

    routing { /* добавим ниже */ }
}
