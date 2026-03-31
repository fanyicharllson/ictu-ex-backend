package com.fanyiadrien.ictuexbackend

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTests {

    @Test
    fun verifyModularity() {
        val modules = ApplicationModules.of(IctuExBackendApplication::class.java)
        modules.verify()
        println(modules)
    }
}