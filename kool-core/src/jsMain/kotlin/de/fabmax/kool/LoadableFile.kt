package de.fabmax.kool

import de.fabmax.kool.util.Uint8Buffer
import de.fabmax.kool.util.Uint8BufferImpl
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.File
import kotlin.js.Promise

actual class LoadableFile(val file: File) {
    actual val name: String
        get() = file.name
    actual val size: Long
        get() = file.size.toLong()
    actual val mimeType: String
        get() = file.type

    actual suspend fun read(): Uint8Buffer {
        val data = file.asDynamic().arrayBuffer() as Promise<ArrayBuffer>
        return Uint8BufferImpl(Uint8Array(data.await()))
    }

    override fun toString(): String {
        return "$name [${(size / (1024.0 * 1024.0)).toString(1)} mb / $mimeType]"
    }
}