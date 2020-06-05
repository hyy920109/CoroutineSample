package com.example.coroutinedemo

object LogUtils {

    fun log(tag: String = "MainActivity", msg: String) {
        println("$tag ---> $msg")
    }
}