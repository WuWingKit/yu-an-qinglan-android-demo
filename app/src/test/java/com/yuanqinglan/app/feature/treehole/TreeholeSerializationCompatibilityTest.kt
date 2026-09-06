/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole

import com.yuanqinglan.app.data.local.AppJson
import com.yuanqinglan.app.feature.treehole.model.HumanLetter
import com.yuanqinglan.app.feature.treehole.model.PetLetter
import com.yuanqinglan.app.feature.treehole.model.TreeholeAvatarStyle
import com.yuanqinglan.app.feature.treehole.model.TreeholeAuthor
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterState
import com.yuanqinglan.app.feature.treehole.model.TreeholePaperStyle
import com.yuanqinglan.app.feature.treehole.ui.formatLetterDate
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #22 序列化兼容与作者展示字段（纯 JVM）：
 * - 旧版本 JSON / 本地快照缺 author 字段（或 author 为 null / 缺子字段）时正常反序列化，
 *   原有标题、正文、分类等内容不丢失；
 * - 新 JSON 完整作者字段正确解析；
 * - 头像 token 缺失/未知回退产品默认；写信日期按中文格式输出。
 */
class TreeholeSerializationCompatibilityTest {

    /** App 1.x 形态的人间信件 JSON：无 author 字段，必须兼容读取。 */
    @Test
    fun oldHumanJsonWithoutAuthorStillDecodesWithoutContentLoss() {
        val raw = """
            {
              "id": "tlh-001",
              "title": "想对你说句晚安",
              "body": "今天下班很晚，路过我们常去的那家面馆，灯还亮着。",
              "category": "思念",
              "paper": "PLAIN",
              "createdAtMillis": 1748900000000,
              "state": "PUBLISHED"
            }
        """.trimIndent()

        val letter = AppJson.decodeFromString(HumanLetter.serializer(), raw)

        // 旧字段完整保留，内容不因新字段丢失
        assertEquals("tlh-001", letter.id)
        assertEquals("想对你说句晚安", letter.title)
        assertEquals("今天下班很晚，路过我们常去的那家面馆，灯还亮着。", letter.body)
        assertEquals("思念", letter.category)
        assertEquals(TreeholePaperStyle.PLAIN, letter.paper)
        assertEquals(1_748_900_000_000L, letter.createdAtMillis)
        assertEquals(TreeholeLetterState.PUBLISHED, letter.state)

        // 新字段以可空默认值兜底（整体缺省 → null），展示回退可用，旧内容不丢
        assertNull(letter.author)
        val fallback = letter.author ?: TreeholeAuthor()
        assertEquals(TreeholeAuthor.DEFAULT_NICKNAME, fallback.displayNickname)
        assertEquals(TreeholeAuthor.DEFAULT_ANON_ID, fallback.displayAnonId)
        assertNull(fallback.avatar)
    }

    /** App 1.x 形态的生灵信件 JSON：无 author 字段，必须兼容读取。 */
    @Test
    fun oldPetJsonWithoutAuthorStillDecodesWithoutContentLoss() {
        val raw = """
            {
              "id": "tlp-001",
              "title": "窗台还留着你的位置",
              "body": "每天清晨我还是会下意识看向窗台。",
              "category": "想念",
              "paper": "GREEN",
              "createdAtMillis": 1749400000000,
              "state": "PUBLISHED"
            }
        """.trimIndent()

        val letter = AppJson.decodeFromString(PetLetter.serializer(), raw)

        assertEquals("tlp-001", letter.id)
        assertEquals("窗台还留着你的位置", letter.title)
        assertEquals("每天清晨我还是会下意识看向窗台。", letter.body)
        assertEquals(TreeholePaperStyle.GREEN, letter.paper)
        assertNull(letter.author)
        val fallback = letter.author ?: TreeholeAuthor()
        assertEquals(TreeholeAuthor.DEFAULT_NICKNAME, fallback.displayNickname)
    }

    /** App 2.0 新 JSON：完整作者字段正确解析。 */
    @Test
    fun newJsonWithFullAuthorDecodes() {
        val raw = """
            {
              "id": "tlh-006",
              "title": "老街的桂花开了",
              "body": "楼下那棵桂花树又开了。",
              "category": "思念",
              "paper": "WARM",
              "createdAtMillis": 1749800000000,
              "state": "PUBLISHED",
              "author": {
                "nickname": "秋日拾桂",
                "anonId": "青澜·8bK1",
                "avatar": "flower"
              }
            }
        """.trimIndent()

        val letter = AppJson.decodeFromString(HumanLetter.serializer(), raw)
        val author = requireNotNull(letter.author)

        assertEquals("秋日拾桂", author.nickname)
        assertEquals("秋日拾桂", author.displayNickname)
        assertEquals("青澜·8bK1", author.anonId)
        assertEquals("青澜·8bK1", author.displayAnonId)
        assertEquals("flower", author.avatar)
        assertEquals(TreeholePaperStyle.WARM, letter.paper)
    }

    /** author 对象缺子字段：逐字段默认，不抛错。 */
    @Test
    fun partialAuthorFallsBackPerField() {
        val raw = """
            {
              "id": "tlh-001",
              "title": "标题",
              "body": "正文",
              "category": "思念",
              "paper": "PLAIN",
              "createdAtMillis": 1,
              "state": "PUBLISHED",
              "author": { "nickname": "只给了昵称" }
            }
        """.trimIndent()

        val letter = AppJson.decodeFromString(HumanLetter.serializer(), raw)
        val author = requireNotNull(letter.author)

        assertEquals("只给了昵称", author.nickname)
        assertEquals("", author.anonId)
        assertNull(author.avatar)
        assertEquals(TreeholeAuthor.DEFAULT_ANON_ID, author.displayAnonId)
    }

    /** author 显式 null：视同缺省 → author 为 null，展示回退默认。 */
    @Test
    fun explicitNullAuthorDecodesToNullWithDisplayFallback() {
        val raw = """
            {
              "id": "tlh-001",
              "title": "标题",
              "body": "正文",
              "category": "思念",
              "paper": "PLAIN",
              "createdAtMillis": 1,
              "state": "PUBLISHED",
              "author": null
            }
        """.trimIndent()

        val letter = AppJson.decodeFromString(HumanLetter.serializer(), raw)
        assertNull(letter.author)
        val fallback = letter.author ?: TreeholeAuthor()
        assertEquals(TreeholeAuthor.DEFAULT_NICKNAME, fallback.displayNickname)
        assertEquals(TreeholeAuthor.DEFAULT_ANON_ID, fallback.displayAnonId)
    }

    /** 本地寄信构建：REVIEWING + 中性默认作者（仓库 builder 行为）。 */
    @Test
    fun submittedLetterKeepsNeutralAuthorAndReviewingState() {
        val letter = HumanLetter(
            id = "tlh-999",
            title = "写给未来的自己",
            body = "慢慢来，也来得及。",
            category = "祝福",
            paper = TreeholePaperStyle.GREEN,
            image = null,
            audio = null,
            createdAtMillis = 1_750_000_000_000L,
            state = TreeholeLetterState.REVIEWING,
        )

        assertNull(letter.author)
        assertEquals(TreeholeLetterState.REVIEWING, letter.state)
    }

    /** 头像 token：缺失/空/未知一律回退产品默认；已知 token 精确匹配。 */
    @Test
    fun avatarTokenFallsBackToProductDefault() {
        assertEquals(TreeholeAvatarStyle.PERSON, TreeholeAvatarStyle.fromToken(null))
        assertEquals(TreeholeAvatarStyle.PERSON, TreeholeAvatarStyle.fromToken(""))
        assertEquals(TreeholeAvatarStyle.PERSON, TreeholeAvatarStyle.fromToken("unknown-token"))
        assertEquals(TreeholeAvatarStyle.LEAF, TreeholeAvatarStyle.fromToken("leaf"))
        assertEquals(TreeholeAvatarStyle.PAW, TreeholeAvatarStyle.fromToken("paw"))
        assertEquals(TreeholeAvatarStyle.WATERDROP, TreeholeAvatarStyle.fromToken("waterdrop"))
    }

    /** 写信日期：中文「yyyy年M月d日」格式（与测试机时区自洽，避免时区抖动）。 */
    @Test
    fun letterDateFormattedAsChineseDate() {
        val millis = 1_749_800_000_000L
        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val expected = "${localDate.year}年${localDate.monthValue}月${localDate.dayOfMonth}日"
        assertEquals(expected, formatLetterDate(millis))
    }

    /** 日期格式化对异常输入不抛错（0 与当前时间）。 */
    @Test
    fun letterDateHandlesZeroAndNow() {
        assertTrue(formatLetterDate(0L).isNotBlank())
        assertTrue(formatLetterDate(System.currentTimeMillis()).isNotBlank())
    }
}
