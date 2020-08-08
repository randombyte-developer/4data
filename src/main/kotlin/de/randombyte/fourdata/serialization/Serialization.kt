package de.randombyte.fourdata.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))