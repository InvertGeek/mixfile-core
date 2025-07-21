package com.donut.mixfile.server.core.uploaders.js

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

class JSFunction<T>(val callback: (args: Array<out Any>) -> T?) : org.mozilla.javascript.Function {

    override fun getClassName(): String {
        return "KotlinJSFunction"
    }

    override fun get(name: String?, start: Scriptable?): Any {
        return ""
    }

    override fun get(index: Int, start: Scriptable?): Any {
        return ""
    }

    override fun has(name: String?, start: Scriptable?): Boolean {
        return false
    }

    override fun has(index: Int, start: Scriptable?): Boolean {
        return false
    }

    override fun put(name: String?, start: Scriptable?, value: Any?) {

    }

    override fun put(index: Int, start: Scriptable?, value: Any?) {

    }

    override fun delete(name: String?) {

    }

    override fun delete(index: Int) {

    }

    override fun getPrototype(): Scriptable {
        return this
    }

    override fun setPrototype(prototype: Scriptable?) {

    }

    override fun getParentScope(): Scriptable {
        return this
    }

    override fun setParentScope(parent: Scriptable?) {

    }

    override fun getIds(): Array<Any> {
        return arrayOf()
    }

    override fun getDefaultValue(hint: Class<*>?): Any {
        return ""
    }

    override fun hasInstance(instance: Scriptable?): Boolean {
        return false
    }

    override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any? {
        return callback(args ?: emptyArray())
    }

    override fun construct(cx: Context?, scope: Scriptable?, args: Array<out Any>?): Scriptable {
        return this
    }
}