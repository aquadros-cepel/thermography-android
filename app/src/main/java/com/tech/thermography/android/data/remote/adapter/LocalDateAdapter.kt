package com.tech.thermography.android.data.remote.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.LocalDate

/**
 * Gson TypeAdapter for java.time.LocalDate
 * Serializes LocalDate to ISO-8601 date string format (yyyy-MM-dd)
 * Deserializes ISO-8601 date string to LocalDate
 */
class LocalDateAdapter : TypeAdapter<LocalDate>() {

    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(`in`: JsonReader): LocalDate? {
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                null
            }
            else -> LocalDate.parse(`in`.nextString())
        }
    }
}
