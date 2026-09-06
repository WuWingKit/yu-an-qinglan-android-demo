/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole

import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.treehole.data.HumanLettersFile
import com.yuanqinglan.app.feature.treehole.data.PetLettersFile
import com.yuanqinglan.app.feature.treehole.data.TreeholePool
import com.yuanqinglan.app.feature.treehole.model.HUMAN_POOL_CATEGORIES
import com.yuanqinglan.app.feature.treehole.model.HumanLetter
import com.yuanqinglan.app.feature.treehole.model.PET_POOL_CATEGORIES
import com.yuanqinglan.app.feature.treehole.model.PetLetter
import com.yuanqinglan.app.feature.treehole.model.TreeholeAuthor
import com.yuanqinglan.app.feature.treehole.model.TreeholeAvatarStyle
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterState
import com.yuanqinglan.app.feature.treehole.model.TreeholePaperStyle
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #22 双池内容门禁（读取真实 assets JSON，纯 JVM）：
 * - 每池可轮换内容 ≥ 8 条，两套文件/DTO/ID 前缀完全隔离（换一封与重启均不串池）；
 * - 每条信件含虚构昵称、非实名 ID、头像 token、分类、标题、正文、时间与已发布状态；
 * - 内容合规：无联系方式/定位/募捐/导流/可识别个人信息，无社交计数字段，无演示字样。
 */
class TreeholePoolContentComplianceTest {

    private val humanRaw = assetJson("src/main/assets/demo/treehole/human-letters.json")
    private val petRaw = assetJson("src/main/assets/demo/treehole/pet-letters.json")

    private val humanLetters: List<HumanLetter> =
        AppJson.decodeFromString(HumanLettersFile.serializer(), humanRaw).letters

    private val petLetters: List<PetLetter> =
        AppJson.decodeFromString(PetLettersFile.serializer(), petRaw).letters

    // ---------- 内容量 ----------

    @Test
    fun eachPoolHasAtLeastEightRotatableLetters() {
        assertTrue(
            "人间池应不少于 8 条可轮换内容，实际 ${humanLetters.size}",
            humanLetters.size >= 8,
        )
        assertTrue(
            "生灵池应不少于 8 条可轮换内容，实际 ${petLetters.size}",
            petLetters.size >= 8,
        )
    }

    // ---------- 必需展示字段 ----------

    @Test
    fun everyLetterCarriesRequiredDisplayFields() {
        val required = listOf("title", "body", "category", "createdAtMillis", "state")

        val humanViolations = humanLetters.mapNotNull { letter ->
            val missing = required.filter { key ->
                when (key) {
                    "title" -> letter.title.isBlank()
                    "body" -> letter.body.isBlank()
                    "category" -> letter.category.isBlank()
                    "createdAtMillis" -> letter.createdAtMillis <= 0L
                    "state" -> letter.state != TreeholeLetterState.PUBLISHED
                    else -> true
                }
            }
            val author = letter.author ?: TreeholeAuthor()
            val authorMissing = buildList {
                if (author.nickname.isBlank()) add("author.nickname")
                if (author.anonId.isBlank()) add("author.anonId")
            }
            val badAvatar = author.avatar != null &&
                TreeholeAvatarStyle.entries.none { it.token == author.avatar }
            (missing + authorMissing + if (badAvatar) listOf("author.avatar(未知 token)") else emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { "${letter.id} 缺字段：$it" }
        }
        assertTrue("人间池字段缺失：$humanViolations", humanViolations.isEmpty())

        val petViolations = petLetters.mapNotNull { letter ->
            val missing = required.filter { key ->
                when (key) {
                    "title" -> letter.title.isBlank()
                    "body" -> letter.body.isBlank()
                    "category" -> letter.category.isBlank()
                    "createdAtMillis" -> letter.createdAtMillis <= 0L
                    "state" -> letter.state != TreeholeLetterState.PUBLISHED
                    else -> true
                }
            }
            val author = letter.author ?: TreeholeAuthor()
            val authorMissing = buildList {
                if (author.nickname.isBlank()) add("author.nickname")
                if (author.anonId.isBlank()) add("author.anonId")
            }
            val badAvatar = author.avatar != null &&
                TreeholeAvatarStyle.entries.none { it.token == author.avatar }
            (missing + authorMissing + if (badAvatar) listOf("author.avatar(未知 token)") else emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { "${letter.id} 缺字段：$it" }
        }
        assertTrue("生灵池字段缺失：$petViolations", petViolations.isEmpty())
    }

    @Test
    fun everyLetterCategoryBelongsToItsOwnPool() {
        assertTrue(
            "人间池分类越界：${humanLetters.map { it.id to it.category }}",
            humanLetters.all { it.category in HUMAN_POOL_CATEGORIES },
        )
        assertTrue(
            "生灵池分类越界：${petLetters.map { it.id to it.category }}",
            petLetters.all { it.category in PET_POOL_CATEGORIES },
        )
        // 两池分类清单互斥（产品边界：不跨池混用）
        assertTrue(HUMAN_POOL_CATEGORIES.none { it in PET_POOL_CATEGORIES })
    }

    // ---------- 双池隔离（换一封/重启不串池） ----------

    @Test
    fun poolIdsAreStrictlyIsolated() {
        assertTrue(humanLetters.isNotEmpty())
        assertTrue(petLetters.isNotEmpty())

        // ID 前缀互斥：人间 tlh-、生灵 tlp-
        assertTrue("人间池出现非 tlh- ID", humanLetters.all { it.id.startsWith("tlh-") })
        assertTrue("人间池混入 tlp- ID", humanLetters.none { it.id.startsWith("tlp-") })
        assertTrue("生灵池出现非 tlp- ID", petLetters.all { it.id.startsWith("tlp-") })
        assertTrue("生灵池混入 tlh- ID", petLetters.none { it.id.startsWith("tlh-") })

        // 非实名 ID 前缀同样隔离：青澜· / 生灵·（真实 JSON 均带作者，缺失即视为不合规）
        assertTrue(
            humanLetters.all {
                requireNotNull(it.author) { "${it.id} 缺少作者" }.anonId.startsWith("青澜·")
            },
        )
        assertTrue(
            petLetters.all {
                requireNotNull(it.author) { "${it.id} 缺少作者" }.anonId.startsWith("生灵·")
            },
        )
    }

    @Test
    fun restartAndRotationStayWithinPool() = runBlocking {
        // 模拟两次启动（重启）：各自重新从真实内容构造独立池
        fun buildHumanPool(): TreeholePool<HumanLetter> = TreeholePool(
            seedProvider = { humanLetters },
            idPrefix = "tlh-",
            categories = HUMAN_POOL_CATEGORIES,
            builder = { id, title, body, category, paper, image, audio, now ->
                HumanLetter(
                    id, title, body, category, paper, image, audio, now,
                    TreeholeLetterState.REVIEWING,
                )
            },
        )

        fun buildPetPool(): TreeholePool<PetLetter> = TreeholePool(
            seedProvider = { petLetters },
            idPrefix = "tlp-",
            categories = PET_POOL_CATEGORIES,
            builder = { id, title, body, category, paper, image, audio, now ->
                PetLetter(
                    id, title, body, category, paper, image, audio, now,
                    TreeholeLetterState.REVIEWING,
                )
            },
        )

        val firstHuman = buildHumanPool()
        val firstPet = buildPetPool()

        val humanPublic = (firstHuman.publicLetters().toList().last()
            as DemoState.Success<List<HumanLetter>>).value
        val petPublic = (firstPet.publicLetters().toList().last()
            as DemoState.Success<List<PetLetter>>).value

        // 公共拾信池只含本池内容（换一封的候选即池内公共内容，不可能跨池）
        assertTrue(humanPublic.isNotEmpty())
        assertTrue(humanPublic.all { it.id.startsWith("tlh-") })
        assertTrue(petPublic.isNotEmpty())
        assertTrue(petPublic.all { it.id.startsWith("tlp-") })
        assertTrue(humanPublic.none { human -> petPublic.any { it.id == human.id } })
        assertTrue(petPublic.none { pet -> humanPublic.any { it.id == pet.id } })

        // 重启模拟：新实例与旧实例内容一致，且仍不串池
        val secondHumanPublic = (buildHumanPool().publicLetters().toList().last()
            as DemoState.Success<List<HumanLetter>>).value
        val secondPetPublic = (buildPetPool().publicLetters().toList().last()
            as DemoState.Success<List<PetLetter>>).value

        assertEquals(humanPublic.map { it.id }, secondHumanPublic.map { it.id })
        assertEquals(petPublic.map { it.id }, secondPetPublic.map { it.id })
        assertTrue(secondHumanPublic.none { it.id.startsWith("tlp-") })
        assertTrue(secondPetPublic.none { it.id.startsWith("tlh-") })
    }

    // ---------- 内容合规 ----------

    @Test
    fun noForbiddenContactLocationOrTrafficContent() {
        val patterns = listOf(
            "手机号" to Regex("1[3-9]\\d{9}"),
            "座机号" to Regex("0\\d{2,3}-?\\d{7,8}"),
            "邮箱" to Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
            "网址" to Regex("(https?://|www\\.)"),
            "身份证号" to Regex("\\d{17}[\\dXx]"),
            "社交导流" to Regex("微信|WeChat|weixin|私信|加我|扫码|公众号|抖音|微博|小红书|直播|群号|拉群|链接"),
            "资金类" to Regex("打赏|收款|转账|银行卡|支付宝|募捐|捐款|众筹|水滴筹|轻松筹|筹款"),
            "定位/地域" to Regex(
                "重庆|渝中|江北|南岸|沙坪坝|九龙坡|渝北|巴南|北碚|大渡口|万州|涪陵|永川|合川|江津|长寿|" +
                    "南川|綦江|大足|铜梁|潼南|荣昌|璧山|梁平|垫江|丰都|忠县|云阳|奉节|巫山|巫溪|开州|" +
                    "石柱|秀山|酉阳|彭水|武隆|城口|黔江|小区|街道|门牌|邮政编码|坐标",
            ),
            "演示字样" to Regex("演示|原型|假数据"),
        )

        val violationMessages = (humanLetters + petLetters).mapNotNull { letter ->
            val author = letter.author ?: TreeholeAuthor()
            val text = listOf(letter.title, letter.body, author.nickname, author.anonId)
                .joinToString(" ")
            val hits = patterns.filter { (_, regex) -> regex.containsMatchIn(text) }
                .map { it.first }
            hits.takeIf { it.isNotEmpty() }?.let { "${letter.id} 命中：$it" }
        }
        assertTrue("内容不合规：$violationMessages", violationMessages.isEmpty())
    }

    @Test
    fun noSocialCountOrRegionFieldsInPayload() {
        val forbiddenKeys = listOf(
            "likes", "likeCount", "hot", "heat", "followers", "fans", "following",
            "region", "city", "location", "geo", "latitude", "longitude", "comments", "reply",
        )
        val lowered = (humanRaw + "\n" + petRaw).lowercase()

        val hits = forbiddenKeys.filter { key -> lowered.contains("\"$key\"") }
        assertTrue(
            "双池 JSON 出现被禁止的社交/地域字段：$hits",
            hits.isEmpty(),
        )
    }

    // ---------- 辅助 ----------

    private fun assetJson(relativePath: String): String {
        val file = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
        ).firstOrNull { it.isFile }
            ?: throw AssertionError(
                "找不到资产文件 $relativePath（工作目录：${System.getProperty("user.dir")}）",
            )
        return file.readText(Charsets.UTF_8)
    }
}
