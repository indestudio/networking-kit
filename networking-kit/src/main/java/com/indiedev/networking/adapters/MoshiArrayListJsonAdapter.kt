package  com.indiedev.networking.adapters

import com.squareup.moshi.*
import com.squareup.moshi.JsonAdapter.Factory
import java.io.IOException
import java.lang.reflect.Type

internal abstract class MoshiArrayListJsonAdapter<C : MutableCollection<T>?, T> private constructor(
    private val elementAdapter: JsonAdapter<T>,
) : JsonAdapter<C>() {
    abstract fun newCollection(): C

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): C {
        val result = newCollection()
        reader.beginArray()
        while (reader.hasNext()) {
            result?.add(elementAdapter.fromJson(reader)!!)
        }
        reader.endArray()
        return result
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: C?) {
        writer.beginArray()
        for (element in value!!) {
            elementAdapter.toJson(writer, element)
        }
        writer.endArray()
    }

    override fun toString(): String {
        return "$elementAdapter.collection()"
    }

    companion object {
        val FACTORY = Factory { type, annotations, moshi ->
            val rawType = Types.getRawType(type)
            if (annotations.isNotEmpty()) return@Factory null
            if (rawType == ArrayList::class.java) {
                return@Factory newArrayListAdapter<Any>(
                    type,
                    moshi,
                ).nullSafe()
            }
            null
        }

        private fun <T> newArrayListAdapter(
            type: Type,
            moshi: Moshi,
        ): JsonAdapter<MutableCollection<T>> {
            val elementType =
                Types.collectionElementType(
                    type,
                    MutableCollection::class.java,
                )

            val elementAdapter: JsonAdapter<T> = moshi.adapter(elementType)

            return object :
                MoshiArrayListJsonAdapter<MutableCollection<T>, T>(elementAdapter) {
                override fun newCollection(): MutableCollection<T> {
                    return ArrayList()
                }
            }
        }
    }
}
