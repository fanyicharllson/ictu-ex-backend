rootProject.name = "ictu-ex-backend"

include("ictu-ex-app")    // This will hold your @SpringBootApplication class
include("auth")
include("listing")
include("notification")
include("shared")         // For shared Kafka event classes
include("messaging")       // For Kafka configuration and utilities