package ru.misterpotz.listly.utils

import java.util.logging.Logger

/** Простейший helper для отладочного логирования в демо-проекте. */
fun Any?.log() {
    println("MyLogTag $this")
}
