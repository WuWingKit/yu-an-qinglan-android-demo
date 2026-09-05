/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial

import com.yuanqinglan.app.feature.memorial.data.DaijiCollectiveStore
import com.yuanqinglan.app.feature.memorial.data.AiFlowGate
import com.yuanqinglan.app.feature.memorial.data.EthicsGateRules
import com.yuanqinglan.app.feature.memorial.model.CollectiveActivity
import com.yuanqinglan.app.feature.memorial.model.DaijiOrderStatus
import com.yuanqinglan.app.feature.memorial.model.DaijiPackage
import com.yuanqinglan.app.feature.memorial.model.MediaKind
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 异地代祭（付费预约+线下履约）与线上集体共祭（免费公益报名）状态完全独立：
 * 各自状态机与集合互不影响；AI 追忆伦理确认不可跳过、目标 ID 衔接不丢失。
 */
class DaijiCollectiveAiTest {

    private val packageBasic = DaijiPackage(
        id = "dj-basic",
        title = "代祭关怀套餐",
        description = "基础代祭",
        priceText = "约 268 元/次起",
        durationText = "约 20 分钟",
        contents = listOf("清扫", "献花"),
    )

    private val activity = CollectiveActivity(
        id = "col-001",
        title = "清明·线上集体共祭",
        description = "公益活动",
        dateText = "4 月 4 日 - 4 月 6 日",
        location = "线上",
    )

    @Test
    fun `代祭订单与共祭报名是两个互不影响的独立状态集合`() = runTest {
        val store = DaijiCollectiveStore(loader = null)

        val order = store.createOrder(
            memorialId = "hm-001",
            memorialName = "陈永昌",
            pkg = packageBasic,
            entrustName = "女儿",
            expectDateText = "2026年4月4日",
            message = "麻烦代为清扫",
        )
        assertNotNull(order)
        assertTrue(order.orderNo.startsWith("YQJ-"))
        assertEquals(DaijiOrderStatus.SUBMITTED, order.status)

        // 报名公益共祭
        val signup = store.signUp(activity)
        assertEquals(activity.id, signup.activityId)

        // 推进代祭订单状态不影响共祭报名
        val advanced = store.advanceOrder(order.id)!!
        assertEquals(DaijiOrderStatus.CONFIRMED, advanced.status)
        assertNotNull(store.signupOf(activity.id))

        // 状态集合类型互斥：orders 里只有 DaijiOrder、signups 里只有报名
        assertTrue(store.orderState.value.values.all { it.orderNo == order.orderNo })
        assertTrue(store.signupState.value.keys.all { it == activity.id })
        assertEquals(1, store.orderState.value.size)
        assertEquals(1, store.signupState.value.size)
    }

    @Test
    fun `履约状态单向推进且可重置`() = runTest {
        val store = DaijiCollectiveStore(loader = null)
        val order = store.createOrder(
            memorialId = "pm-001",
            memorialName = "年糕",
            pkg = packageBasic,
            entrustName = "家人",
            expectDateText = "2026年4月5日",
            message = "",
        )
        assertEquals(DaijiOrderStatus.SUBMITTED, order.status)

        val confirmed = store.advanceOrder(order.id)!!
        assertEquals(DaijiOrderStatus.CONFIRMED, confirmed.status)
        val completed = store.advanceOrder(order.id)!!
        assertEquals(DaijiOrderStatus.COMPLETED, completed.status)
        // 终态幂等
        assertEquals(DaijiOrderStatus.COMPLETED, store.advanceOrder(order.id)!!.status)

        // 可重置以便本地重复查看流程
        assertEquals(DaijiOrderStatus.SUBMITTED, store.resetOrderProgress(order.id)!!.status)
        // 不存在的订单返回 null
        assertNull(store.advanceOrder("no-such"))
    }

    @Test
    fun `履约影像归档只落在订单与所属空间不混入报名`() = runTest {
        val store = DaijiCollectiveStore(loader = null)
        val order = store.createOrder(
            memorialId = "hm-002",
            memorialName = "林静萱",
            pkg = packageBasic,
            entrustName = "子女",
            expectDateText = "2026年4月6日",
            message = "",
        )
        val photo = MediaRef(
            id = "arch-1",
            kind = MediaKind.IMAGE_FILE,
            value = "file:///private/arch-1.webp",
            name = "履约影像",
            sizeBytes = 100,
        )
        val updated = store.archiveImagesToOrder(order.id, listOf(photo))!!
        assertEquals(1, updated.archiveImages.size)
        assertEquals("arch-1", updated.archiveImages.first().id)
        // 报名集合不因归档而变化
        assertTrue(store.signupState.value.isEmpty())
    }

    @Test
    fun `取消共祭报名不影响代祭订单`() = runTest {
        val store = DaijiCollectiveStore(loader = null)
        store.createOrder(
            memorialId = "hm-001",
            memorialName = "陈永昌",
            pkg = packageBasic,
            entrustName = "女儿",
            expectDateText = "2026年4月4日",
            message = "",
        )
        store.signUp(activity)
        assertTrue(store.cancelSignup(activity.id))
        assertFalse(store.cancelSignup(activity.id))
        assertEquals(0, store.signupState.value.size)
        assertEquals(1, store.orderState.value.size)
    }

    @Test
    fun `共祭目录与代祭目录独立加载`() = runTest {
        // loader=null 时目录为空态而非报错，保证两个目录流互不影响
        val store = DaijiCollectiveStore(loader = null)
        assertEquals(com.yuanqinglan.app.core.model.DemoState.Empty, store.packages().last())
        assertEquals(com.yuanqinglan.app.core.model.DemoState.Empty, store.activities().last())
    }

    @Test
    fun `AI 伦理不可跳过-必须勾选阅读确认`() {
        assertFalse(EthicsGateRules.mayProceed(readConfirmed = false))
        assertTrue(EthicsGateRules.mayProceed(readConfirmed = true))
    }

    @Test
    fun `AI 门-未同意不可进入且目标 ID 经无参伦理页衔接不丢`() {
        AiFlowGate.resetSession()
        assertFalse(AiFlowGate.consented.value)

        AiFlowGate.prepare("hm-001")
        assertEquals("hm-001", AiFlowGate.pendingMemorialId.value)

        // 未同意时消费不成立：模拟用户直接返回
        AiFlowGate.resetSession()
        assertNull(AiFlowGate.consumePending())

        // 同意后消费到目标
        AiFlowGate.prepare("pm-001")
        AiFlowGate.grantConsent()
        assertTrue(AiFlowGate.consented.value)
        assertEquals("pm-001", AiFlowGate.consumePending())
        assertNull(AiFlowGate.pendingMemorialId.value)
        AiFlowGate.resetSession()
    }
}
