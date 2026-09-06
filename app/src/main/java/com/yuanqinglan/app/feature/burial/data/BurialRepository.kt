/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.burial.data

import com.yuanqinglan.app.core.model.AudienceTrack
import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.DemoAssetLoader
import com.yuanqinglan.app.feature.burial.model.BurialMode
import com.yuanqinglan.app.feature.burial.model.BurialOrder
import com.yuanqinglan.app.feature.burial.model.BurialPlan
import com.yuanqinglan.app.feature.burial.model.HumanBurialService
import com.yuanqinglan.app.feature.burial.model.OrderProgress
import com.yuanqinglan.app.feature.burial.model.OrderNumberGenerator
import com.yuanqinglan.app.feature.burial.model.PetBurialService
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer

/** 创建订单所需的强类型草稿（轨道类型随 audience 固定，关键字段不跨轨）。 */
data class BurialOrderDraft(
    val audience: AudienceTrack,
    val serviceId: String,
    val serviceName: String,
    val mode: BurialMode,
    val planId: String,
    val planTitle: String,
    val amountText: String,
    val planPriceYuan: Int? = null,
    val prepaidYears: Int = 0,
    val prepaidManagementYuan: Int = 0,
    val selectedAddOns: List<String> = emptyList(),
    val addOnYuan: Int = 0,
    val subsidyYuan: Int = 0,
    val totalYuan: Int? = null,
    val managementExpiresYear: Int? = null,
    val renewalAnnualYuan: Int = 0,
    /** 人类轨：逝者姓名；宠物轨：宠物昵称。由调用方按轨道填对应语义值。 */
    val deceasedName: String,
    val contactName: String,
    val phone: String,
    val expectDate: LocalDate?,
)

/**
 * 安葬业务仓库：按轨道返回不同强类型数据流，切换轨道绝不串数据；
 * 每个订阅都先发 Loading、再由公共加载器延时出结果，使加载/成功/空/失败/重试可复现。
 *
 * JSON 由公共数据层 [DemoAssetLoader] 读取（本仓库唯一的公共层依赖点）；
 * 资产路径相对 assets/demo/ 传入（加载器自动补 demo/ 前缀）。
 */
class BurialRepository(
    private val loader: DemoAssetLoader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val latencyMs: Long = 200L,
    private val numberGenerator: OrderNumberGenerator = OrderNumberGenerator(),
) {

    private val orderStore = MutableStateFlow<Map<String, BurialOrder>>(emptyMap())
    private val seq = AtomicInteger(1)

    /** 人类生态安葬服务（强类型，不含宠物项）。 */
    fun humanServices(): Flow<DemoState<List<HumanBurialService>>> = flow {
        emit(DemoState.Loading)
        emit(
            loadChecked("burial/services_human.json", HumanServiceFile.serializer()) { file ->
                file.services.map { it.toDomain() }
            },
        )
    }.flowOn(ioDispatcher)

    /** 宠物生态安葬服务（强类型，不含人类项）。 */
    fun petServices(): Flow<DemoState<List<PetBurialService>>> = flow {
        emit(DemoState.Loading)
        emit(
            loadChecked("burial/services_pet.json", PetServiceFile.serializer()) { file ->
                file.services.map { it.toDomain() }
            },
        )
    }.flowOn(ioDispatcher)

    /** 人类套餐（audience = HUMAN，强类型）。 */
    fun humanPlans(): Flow<DemoState<List<BurialPlan>>> = flow {
        emit(DemoState.Loading)
        emit(
            loadChecked("burial/plans_human.json", BurialPlansFile.serializer()) { file ->
                file.plans.map { it.toDomain(AudienceTrack.HUMAN) }
            },
        )
    }.flowOn(ioDispatcher)

    /** 宠物套餐（audience = PET，强类型）。 */
    fun petPlans(): Flow<DemoState<List<BurialPlan>>> = flow {
        emit(DemoState.Loading)
        emit(
            loadChecked("burial/plans_pet.json", BurialPlansFile.serializer()) { file ->
                file.plans.map { it.toDomain(AudienceTrack.PET) }
            },
        )
    }.flowOn(ioDispatcher)

    // ---------------- 订单（本地内存，可推进/重置，不提交外部） ----------------

    /** 生成本地订单并保存，返回订单（orderId 即订单号）。 */
    suspend fun createOrder(draft: BurialOrderDraft): BurialOrder {
        delay(latencyMs)
        val orderNo = numberGenerator.next(seq.getAndIncrement())
        val now = System.currentTimeMillis()
        val order = BurialOrder(
            id = orderNo,
            orderNo = orderNo,
            audience = draft.audience,
            serviceId = draft.serviceId,
            serviceName = draft.serviceName,
            planId = draft.planId,
            planTitle = draft.planTitle,
            mode = draft.mode,
            deceasedName = draft.deceasedName,
            contactName = draft.contactName,
            phone = draft.phone,
            expectDate = draft.expectDate,
            amountText = draft.amountText,
            planPriceYuan = draft.planPriceYuan,
            prepaidYears = draft.prepaidYears,
            prepaidManagementYuan = draft.prepaidManagementYuan,
            selectedAddOns = draft.selectedAddOns,
            addOnYuan = draft.addOnYuan,
            subsidyYuan = draft.subsidyYuan,
            totalYuan = draft.totalYuan,
            managementExpiresYear = draft.managementExpiresYear,
            renewalAnnualYuan = draft.renewalAnnualYuan,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        orderStore.update { it + (order.id to order) }
        return order
    }

    /** 按订单号观察订单；不存在时发 [DemoState.Empty]。 */
    fun observeOrder(orderId: String): Flow<DemoState<BurialOrder>> =
        orderStore.map { map ->
            map[orderId]?.let { DemoState.Success(it) } ?: DemoState.Empty
        }.distinctUntilChanged()

    /** 推进订单到下一合法状态（终态幂等）。返回更新后订单；订单不存在返回 null。 */
    suspend fun advanceOrder(orderId: String): BurialOrder? =
        updateOrder(orderId) { it.copyStatus(OrderProgress.advance(it.status)) }

    /** 重置订单进度到初始状态（本地重复查看流程）。返回更新后订单；不存在返回 null。 */
    suspend fun resetOrderProgress(orderId: String): BurialOrder? =
        updateOrder(orderId) { it.copyStatus(OrderProgress.reset()) }

    private suspend fun updateOrder(
        orderId: String,
        transform: (BurialOrder) -> BurialOrder,
    ): BurialOrder? {
        orderStore.update { map ->
            map[orderId]?.let { existing -> map + (orderId to transform(existing)) } ?: map
        }
        return orderStore.value[orderId]
    }

    // ---------------- 私有加载辅助 ----------------

    private suspend fun <T, R> loadChecked(
        path: String,
        serializer: KSerializer<T>,
        map: (T) -> List<R>,
    ): DemoState<List<R>> = try {
        val file = loader.load(path, serializer)
        val items = map(file)
        if (items.isEmpty()) DemoState.Empty else DemoState.Success(items)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DemoState.Error("相关信息加载失败，请稍后重试。")
    }
}
