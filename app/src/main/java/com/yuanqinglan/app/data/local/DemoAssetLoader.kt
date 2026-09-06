/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.data.local

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * 全局 JSON 解码器：未知字段忽略、默认值参与编码。
 * 供本地数据（assets/demo 与序列化文件）统一解析。
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * 本地 JSON 加载器：从 `assets/demo/**/**.json` 读取并按 [deserializer] 解析为
 * 任意 @Serializable 类型。在 App/MainActivity 启动时用 Android Context 初始化
 * （通常经 [AppContainer.demoAssets] 获取）。
 */
class DemoAssetLoader(
    private val context: Context,
    private val delayMillis: Long = DEFAULT_SIMULATED_DELAY_MILLIS,
) {

    /**
     * 读取并解析 `assets/demo/<path>` 下的 JSON 文件。
     * [path] 为相对 demo 目录的 assets 路径（如 `home/news.json`）。
     * 注入固定短延迟，使加载中状态可复现。
     *
     * @throws java.io.IOException 文件缺失或读取失败时抛出（含明确文件名信息）。
     */
    suspend fun <T> load(path: String, deserializer: KSerializer<T>): T {
        if (delayMillis > 0L) delay(delayMillis)
        val raw = readRaw(path)
        return AppJson.decodeFromString(deserializer, raw)
    }

    private fun readRaw(path: String): String {
        val normalized = path.trimStart('/')
        val fileName = "demo/$normalized"
        return try {
            context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            throw java.io.IOException("无法读取本地数据文件 assets/$fileName：${e.message}", e)
        }
    }

    companion object {
        const val DEFAULT_SIMULATED_DELAY_MILLIS = 400L
    }
}
