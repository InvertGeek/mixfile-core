package com.donut.mixfile.server.core.objects

import com.donut.mixfile.server.core.routes.api.webdav.objects.normalPath
import com.donut.mixfile.server.core.routes.api.webdav.objects.xml
import com.donut.mixfile.server.core.utils.ConcurrentHashMapSerializer
import com.donut.mixfile.server.core.utils.parseFileMimeType
import com.donut.mixfile.server.core.utils.sanitizeFileName
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap


fun WebDavFile.toDataLog() = FileDataLog(shareInfoData, getName(), size)

@Serializable
// WebDAV 文件类，包含额外属性
data class WebDavFile(
    private var name: String,
    val size: Long = 0,
    val shareInfoData: String = "",
    val isFolder: Boolean = false,
    @Serializable(with = ConcurrentHashMapSerializer::class)
    val files: ConcurrentHashMap<String, WebDavFile> = ConcurrentHashMap(),
    var lastModified: Long = System.currentTimeMillis()
) {

    init {
        sanitizeName()
    }

    fun setName(name: String) {
        this.name = name
        sanitizeName()
    }

    fun getName() = name

    private fun sanitizeName() {
        name = name.trim().sanitizeFileName()
    }

    fun clone(): WebDavFile {

        val newFiles = ConcurrentHashMap<String, WebDavFile>()

        files.forEach { (key, file) ->
            newFiles[key] = file.clone()
        }

        return copy(files = newFiles)
    }

    fun addFile(file: WebDavFile) {
        if (!this.isFolder) {
            return
        }
        val existingFile = files[file.name]
        if (existingFile != null && existingFile.isFolder && file.isFolder) {
            file.files.forEach { (name, subFile) ->
                if (subFile.isFolder) {
                    existingFile.addFile(subFile)
                    return@forEach
                }
                existingFile.files[name] = subFile.clone()
            }
            return
        }
        files[file.name] = file.clone()
    }

    fun listFiles() = files.values.toList()


    fun getLastModifiedFormatted(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date(lastModified))
    }


    fun toXML(path: String, isRoot: Boolean = false): String {
        val pathName = name.takeIf { !isRoot } ?: ""
        if (isFolder) {
            return xml("D:response") {
                "D:href" {
                    -"/${"$path/${pathName}".normalPath()}/".encodeURLPath(encodeEncoded = true)
                }
                "D:propstat" {
                    "D:prop" {
                        "D:displayname" {
                            -name.encodeURLParameter()
                        }
                        "D:resourcetype" {
                            "D:collection" {
                                attribute("xmlns:D", "DAV:")
                            }
                        }

                        "D:getlastmodified" {
                            -getLastModifiedFormatted()
                        }
                    }
                    "D:status" {
                        -"HTTP/1.1 200 OK"
                    }
                }
            }.toString()
        }
        return xml("D:response") {
            "D:href" {
                -"/${"$path/$name".normalPath()}".encodeURLPath(encodeEncoded = true)
            }
            "D:propstat" {
                "D:prop" {
                    "D:displayname" {
                        -name.encodeURLParameter()
                    }
                    "D:resourcetype" {

                    }
                    "D:getcontenttype" {
                        -name.parseFileMimeType().toString()
                    }
                    "D:getcontentlength" {
                        -size.toString()
                    }
                    "D:getetag" {
                        -shareInfoData
                    }
                    "D:getlastmodified" {
                        -getLastModifiedFormatted()
                    }
                }
                "D:status" {
                    -"HTTP/1.1 200 OK"
                }
            }
        }.toString()
    }

}