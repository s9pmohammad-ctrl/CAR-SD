package com.carsd.app

import java.io.BufferedReader
import java.io.InputStreamReader

object RootShell {
    fun hasRoot(): Boolean = runCatching {
        val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        p.exitValue() == 0 && out.contains("uid=0")
    }.getOrDefault(false)

    fun exec(command: String): Result<String> = runCatching {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val text = BufferedReader(InputStreamReader(p.inputStream)).readText()
        val code = p.waitFor()
        if (code != 0) error("root command failed ($code): $text")
        text.trim()
    }
}
