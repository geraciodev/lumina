package com.geraciodev.lumina

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform