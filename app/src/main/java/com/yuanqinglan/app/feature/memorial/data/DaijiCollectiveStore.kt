/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.memorial.data

import com.yuanqinglan.app.core.model.DemoState
import com.yuanqinglan.app.data.local.DemoAssetLoader
import com.yuanqinglan.app.feature.memorial.model.CollectiveActivity
import com.yuanqinglan.app.feature.memorial.model.CollectiveSignup
import com.yuanqinglan.app.feature.memorial.model.DaijiOrder
import com.yuanqinglan.app.feature.memorial.model.DaijiOrderNumberGenerator
import com.yuanqinglan.app.feature.memorial.model.DaijiOrderProgress
import com.yuanqinglan.app.feature.memorial.model.DaijiPackage
import com.yuanqinglan.app.feature.memorial.model.MediaRef
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer

/**
 * 异地代祭（付费预约 + 线下履约）与线上集体共祭（免费公益活动报名）的独立本地存储。
 *
 * 两个业务流从模型、目录到状态集合全部隔离：
 * - [DaijiOrder] 只存在于本 store 的 orders 集合，绑定具体纪念空间；
 * - [CollectiveSignup] 只存在于本 store 的 signups 集合，绑定公益活动；
 * - 二者没有任何共享状态机或共享集合，页面也互不跳转混用。
 *
 * [loader] 为可空目录数据源：真实运行由定位器注入 DemoAssetLoader；
 * 纯逻辑测试可传 null（此时目录流返回空态，不影响订单/报名的本地状态验证）。
 */
class DaijiCollectiveStore(
    private val loader: DemoAssetLoader? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val seq = AtomicInteger(1)
    private val numberGenerator = DaijiOrderNumberGenerator()

    // ---------------- 代祭套餐目录（展示性内容，加载态可复现） ----------------

    fun packages(): Flow<DemoState<List<DaijiPackage>>> = flow {
        emit(DemoState.Loading)
        val source = loader ?: run {
            emit(DemoState.Empty)
            return@flow
        }
        emit(
            loadChecked(source, "memorial/daiji_packages.json", DaijiPackagesFile.serializer()) { file ->
                file.packages
            },
        )
    }.flowOn(ioDispatcher)

    // ---------------- 代祭订单（本地状态机，可推进/重置） ----------------

    private val orders = MutableStateFlow<Map<String, DaijiOrder>>(emptyMap())
    val orderState: StateFlow<Map<String, DaijiOrder>> = orders.asStateFlow()

    /** 某纪念空间的代祭订单（新→旧）。 */
    fun ordersFor(memorialId: String): Flow<List<DaijiOrder>> = orders.map { map ->
        map.values.filter { it.memorialId == memorialId }
            .sortedByDescending { it.createdAtMillis }
    }

    fun order(orderId: String): DaijiOrder? = orders.value[orderId]

    /** 提交代祭预约（本地生成订单号，不提交外部）。 */
    suspend fun createOrder(
        memorialId: String,
        memorialName: String,
        pkg: DaijiPackage,
        entrustName: String,
        expectDateText: String,
        message: String,
    ): DaijiOrder {
        val now = System.currentTimeMillis()
        val orderNo = numberGenerator.next(seq.getAndIncrement())
        val order = DaijiOrder(
            id = orderNo,
            orderNo = orderNo,
            memorialId = memorialId,
            memorialName = memorialName,
            packageId = pkg.id,
            packageTitle = pkg.title,
            priceText = pkg.priceText,
            entrustName = entrustName.trim(),
            expectDateText = expectDateText.trim(),
            message = message.trim(),
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        orders.update { it + (order.id to order) }
        return order
    }

    /** 推进履约状态（终态幂等）；订单不存在返回 null。 */
    suspend fun advanceOrder(orderId: String): DaijiOrder? =
        updateOrder(orderId) { it.copyStatus(DaijiOrderProgress.advance(it.status)) }

    /** 重置履约状态到初始（本地可重复查看流程）。 */
    suspend fun resetOrderProgress(orderId: String): DaijiOrder? =
        updateOrder(orderId) { it.copyStatus(DaijiOrderProgress.reset()) }

    /** 履约影像归档（引用已落私有目录的 MediaRef）。 */
    suspend fun archiveImagesToOrder(orderId: String, images: List<MediaRef>): DaijiOrder? =
        updateOrder(orderId) { it.addArchiveImages(images) }

    private suspend fun updateOrder(
        orderId: String,
        transform: (DaijiOrder) -> DaijiOrder,
    ): DaijiOrder? {
        orders.update { map ->
            map[orderId]?.let { map + (orderId to transform(it)) } ?: map
        }
        return orders.value[orderId]
    }

    // ---------------- 线上集体共祭活动目录（免费公益活动） ----------------

    fun activities(): Flow<DemoState<List<CollectiveActivity>>> = flow {
        emit(DemoState.Loading)
        val source = loader ?: run {
            emit(DemoState.Empty)
            return@flow
        }
        emit(
            loadChecked(source, "memorial/collective_activities.json", CollectiveActivitiesFile.serializer()) { file ->
                file.activities
            },
        )
    }.flowOn(ioDispatcher)

    // ---------------- 共祭报名（本地状态，一个活动一条） ----------------

    private val signups = MutableStateFlow<Map<String, CollectiveSignup>>(emptyMap())
    val signupState: StateFlow<Map<String, CollectiveSignup>> = signups.asStateFlow()

    /** 我的共祭报名历史（新→旧）。 */
    fun mySignups(): Flow<List<CollectiveSignup>> = signups.map { map ->
        map.values.sortedByDescending { it.joinedAtMillis }
    }

    fun signupOf(activityId: String): CollectiveSignup? = signups.value[activityId]

    /** 报名（同活动重复报名幂等，仅保留一条）。 */
    suspend fun signUp(activity: CollectiveActivity): CollectiveSignup {
        val now = System.currentTimeMillis()
        val signup = CollectiveSignup(
            id = "col-signup-${activity.id}-$now",
            activityId = activity.id,
            activityTitle = activity.title,
            joinedAtMillis = now,
        )
        signups.update { it + (activity.id to signup) }
        return signup
    }

    /** 取消报名；不存在返回 false。 */
    suspend fun cancelSignup(activityId: String): Boolean {
        var existed = false
        signups.update { map ->
            if (map.containsKey(activityId)) {
                existed = true
                map - activityId
            } else {
                map
            }
        }
        return existed
    }

    private suspend fun <T, R> loadChecked(
        source: DemoAssetLoader,
        path: String,
        serializer: KSerializer<T>,
        map: (T) -> List<R>,
    ): DemoState<List<R>> = try {
        val items = map(source.load(path, serializer))
        if (items.isEmpty()) DemoState.Empty else DemoState.Success(items)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DemoState.Error("相关信息加载失败，请稍后重试。")
    }
}
