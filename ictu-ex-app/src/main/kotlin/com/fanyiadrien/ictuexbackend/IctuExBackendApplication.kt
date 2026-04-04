package com.fanyiadrien.ictuexbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan("com.fanyiadrien") // Scan all fanyiadrien packages for @Entity
@EnableJpaRepositories("com.fanyiadrien") // Scan all fanyiadrien packages for Repositories
class IctuExBackendApplication

fun main(args: Array<String>) {
    runApplication<IctuExBackendApplication>(*args)
}
