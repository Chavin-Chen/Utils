import io.github.chavin.util.lru.LruFilesCache
import java.io.File
import java.io.FileFilter
import java.io.PrintStream

fun main() {
    val cache = LruFilesCache("/Users/chavinchen/Desktop/cache/", 2, filter = object : FileFilter {
        override fun accept(file: File?): Boolean {
            return !(file?.name?.startsWith(".") ?: true)
        }

    })
    cache.getOrAdd("test")
    cache.getOrAdd("hello").also {
        PrintStream(File(it, "data.txt")).println("Hello world")
    }
    cache.getOrAdd("world").also { // 淘汰 test/
        PrintStream(File(it, "data.md")).println("# Hello LRU")
    }
}
