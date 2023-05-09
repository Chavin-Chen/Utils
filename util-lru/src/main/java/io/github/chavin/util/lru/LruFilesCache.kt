package io.github.chavin.util.lru

import androidx.annotation.WorkerThread
import androidx.collection.LruCache
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.*

class LruFilesCache
/**
 * 创建管理器，并将根路径的下级路径添加到LRU表
 *
 * @param rootPath 根路径
 * @param total 总大小，值为数量或字节数
 * @param cntByte 若为true则统计文件字节数，否则仅统计子目录数
 * @param filter 创建LRU表时用的文件过滤
 */(rootPath: String, total: Int, cntByte: Boolean = false, filter: FileFilter? = null) {
    private val mRootFolder: File

    private val mTotal: Int = total

    private val mCntByte: Boolean = cntByte

    private val mFilter: FileFilter? = filter

    private val mCache: LruCache<String, File>

    init {
        mRootFolder = File(rootPath)
        mCache = createCache(total, cntByte, filter)
    }

    /**
     * 在RootPath下新增一个目录
     */
    fun add(dirName: String): File {
        val folder = File(mRootFolder, dirName)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        mCache.put(dirName, folder)
        return folder
    }

    /**
     * 移除一个子目录
     */
    fun remove(dirName: String): File? {
        return mCache.remove(dirName)
    }

    /**
     * 获取子目录
     */
    operator fun get(dirName: String): File? {
        return mCache.get(dirName)
    }

    /**
     * 获取子目录；不存在时则创建
     */
    fun getOrAdd(dirName: String): File {
        val folder = this[dirName]
        if (null != folder) {
            return folder
        }
        synchronized(mCache) {
            return this[dirName] ?: return add(dirName);
        }
    }

    /**
     * 清理所有
     */
    @WorkerThread
    fun clear() {
        this.delFile(mRootFolder, false)
        mCache.evictAll()
    }

    // ==================================================== private ====================================================
    private fun createCache(total: Int, cntByte: Boolean, filter: FileFilter?): LruCache<String, File> {
        val cache = object : LruCache<String, File>(total.coerceAtLeast(1)) {
            override fun entryRemoved(evicted: Boolean, key: String, oldValue: File, newValue: File?) {
                println("removed:$key, $oldValue")
                this@LruFilesCache.delFile(oldValue, true)
            }

            override fun sizeOf(key: String, value: File): Int {
                return if (cntByte) {
                    this@LruFilesCache.getFileSize(value).toInt()
                } else 1
            }
        }
        if (!mRootFolder.exists() && mRootFolder.mkdirs()) { // 新创建
            return cache
        }
        val folders = mRootFolder.listFiles(filter)
        if (folders.isNullOrEmpty()) { // 空文件夹
            return cache
        }
        // 按远到近添加
        Arrays.sort(folders) { o1: File, o2: File -> (o1.lastModified() - o2.lastModified()).toInt() }
        folders.forEach { cache.put(it.name, it) }
        return cache
    }

    private fun delFile(file: File?, deleteSelf: Boolean) {
        if (null == file || !file.exists()) {
            return
        }
        if (!file.isDirectory) {
            file.delete()
            return
        }
        val files = file.listFiles()
        if (!files.isNullOrEmpty()) {
            files.forEach {
                delFile(it, true);
            }
        }
        if (deleteSelf) {
            file.delete()
        }
    }

    private fun getFileSize(file: File?): Long {
        if (null == file || !file.exists()) {
            return 0L
        }
        if (!file.isDirectory) {
            return file.length()
        }
        val files = file.listFiles()
        if (files.isNullOrEmpty()) {
            return 0L
        }
        return files.sumOf { getFileSize(it) }
    }
}