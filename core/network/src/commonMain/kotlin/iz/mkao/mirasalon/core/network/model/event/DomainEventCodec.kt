package iz.mkao.mirasalon.core.network.model.event

import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import kotlinx.serialization.json.Json

object DomainEventCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(event: DomainEvent): String {
        return json.encodeToString(event)
    }

    fun decode(jsonString: String): DomainEvent {
        return json.decodeFromString(jsonString)
    }
}
