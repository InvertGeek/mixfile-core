package com.donut.mixfile.server.core

import com.donut.mixfile.server.core.Uploader.Companion.registerUrlTransform
import com.donut.mixfile.server.core.utils.decodeHex

val String.sCode: String
    get() = decodeHex(this)

fun initPatchTransforms() {
    //旧文件兼容
    registerUrlTransform("A2") {
        val domain =
            "CO︆︈︇︄︇︄︇︀︇︃︃︊︂️︂️︇︀︆︌︆︁︇︄︂︍︇︃︆︈︂︍︆︃︆️︆︍︆︍︇︅︆︎︆︉︇︄︇︉︂︍︇︀︇︂︆️︆︄︂︍︇︅︇︀︆︌︆️︆︁︆︄︂︍︇︅︆︇︆︃︂︎︆️︇︃︇︃︂︍︆︃︆︎︂︍︇︃︆︈︆︁︆︎︆︇︆︈︆︁︆︉︂︎︆︁︆︌︆︉︇︉︇︅︆︎︆︃︇︃︂︎︆︃︆️︆︍︂️gP".sCode
        if (it.startsWith(domain)) {
            it.replace(
                domain,
                "d5︆︈︇︄︇︄︇︀︇︃︃︊︂️︂️︇︅︇︀︆︌︆️︆︁︆︄︂︍︆︂︆︂︇︃︂︎︆︍︆︉︇︉︆️︇︅︇︃︆︈︆︅︂︎︆︃︆️︆︍︂️w3".sCode
            )
        } else {
            it
        }
    }
}