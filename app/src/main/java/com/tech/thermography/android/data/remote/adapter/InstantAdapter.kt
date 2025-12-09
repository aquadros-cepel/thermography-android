package com.tech.thermography.android.data.remote.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant

/**
 * Gson TypeAdapter for java.time.Instant
 * Serializes Instant to ISO-8601 string format
 * Deserializes ISO-8601 string to Instant
 */
class InstantAdapter : TypeAdapter<Instant>() {

    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(`in`: JsonReader): Instant? {
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                null
            }
            else -> Instant.parse(`in`.nextString())
        }
    }
}
