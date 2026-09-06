/*
 * Copyright (c) 2026 西南大学24级学行科创班胡荣杰（WuWingKit）
 * 本代码著作权归西南大学24级学行科创班胡荣杰（WuWingKit）所有，
 * 未经书面授权禁止另做他用（包括商用和非商用）。
 */

package com.yuanqinglan.app.feature.treehole.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuanqinglan.app.data.local.AppContainer
import com.yuanqinglan.app.data.local.FileStorage
import com.yuanqinglan.app.feature.treehole.data.TreeholePool
import com.yuanqinglan.app.feature.treehole.model.KindResponse
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachment
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachmentKind
import com.yuanqinglan.app.feature.treehole.model.TreeholeAttachmentLimits
import com.yuanqinglan.app.feature.treehole.model.TreeholeLetterLike
import com.yuanqinglan.app.feature.treehole.model.TreeholePaperStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 池页面三段模式：寄信 / 拾信 / 我的信件。 */
enum class TreeholePoolTab {
    WRITE,
    READ,
    MINE,
}

/** 池页面整体 UI 状态（三个模式的表单/展示状态合并到一处便于互斥切换）。 */
data class TreeholePoolUiState(
    val tab: TreeholePoolTab = TreeholePoolTab.WRITE,
    // 寄信草稿
    val paper: TreeholePaperStyle = TreeholePaperStyle.PLAIN,
    val category: String? = null,
    val title: String = "",
    val body: String = "",
    val image: TreeholeAttachment? = null,
    val audio: TreeholeAttachment? = null,
    val titleError: String? = null,
    val bodyError: String? = null,
    val categoryError: String? = null,
    val attachmentNote: String? = null,
    val importing: Boolean = false,
    val submitting: Boolean = false,
    val recording: Boolean = false,
    // 拾信展示
    val currentLetter: TreeholeLetterLike? = null,
    val responseMessage: String? = null,
    // 全页一次性提示条
    val infoBanner: String? = null,
)

/**
 * 树洞内容池页面 ViewModel：人间/生灵两池各建一个实例（各自持有独立池引用，
 * 状态互不共享）。寄信草稿、拾信随机选取与轻回应、我的信件删除均在此维护。
 */
class TreeholePoolViewModel(
    application: Application,
    private val pool: TreeholePool<out TreeholeLetterLike>,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TreeholePoolUiState())
    val uiState: StateFlow<TreeholePoolUiState> = _uiState.asStateFlow()

    private val audioRecorder = TreeholeAudioRecorder(getApplication())
    private var attachmentSeq = 0L

    /** 本会话内已展示过的拾信 ID（避开近期重复）。 */
    private val readHistory = mutableListOf<String>()

    private val repositoryFileStorage: FileStorage
        get() = AppContainer.fileStorage

    // ---------- 模式切换 ----------

    fun selectTab(tab: TreeholePoolTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    // ---------- 寄信草稿 ----------

    fun selectPaper(style: TreeholePaperStyle) {
        _uiState.update { it.copy(paper = style) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(category = category, categoryError = null) }
    }

    fun onTitleChange(text: String) {
        _uiState.update { it.copy(title = text.take(30), titleError = null) }
    }

    fun onBodyChange(text: String) {
        _uiState.update { it.copy(body = text.take(600), bodyError = null) }
    }

    /** 系统选择器返回图片 Uri 后导入：读字节 → 校验 ≤10MB → 落盘 → 挂到草稿。 */
    fun importImageFromUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(importing = true, attachmentNote = null) }
            val error = importAttachment(TreeholeAttachmentKind.IMAGE, uri)
            _uiState.update { state ->
                if (error != null) {
                    state.copy(importing = false, attachmentNote = error)
                } else {
                    state.copy(importing = false)
                }
            }
        }
    }

    /** 系统选择器返回音频 Uri 后导入：读字节 → 校验 ≤5MB → 落盘 → 挂到草稿。 */
    fun importAudioFromUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(importing = true, attachmentNote = null) }
            val error = importAttachment(TreeholeAttachmentKind.AUDIO, uri)
            _uiState.update { state ->
                if (error != null) {
                    state.copy(importing = false, attachmentNote = error)
                } else {
                    state.copy(importing = false)
                }
            }
        }
    }

    private suspend fun importAttachment(kind: TreeholeAttachmentKind, uri: Uri): String? {
        val context = getApplication<Application>()
        val bytes = readContentBytes(context, uri) ?: return "读取文件失败，请重试"
        val limitError = when (kind) {
            TreeholeAttachmentKind.IMAGE -> TreeholeAttachmentLimits.imageErrorIfAny(bytes.size.toLong())
            TreeholeAttachmentKind.AUDIO -> TreeholeAttachmentLimits.audioErrorIfAny(bytes.size.toLong())
        }
        // 超限只提示、不落盘。
        if (limitError != null) return limitError
        val fallbackName = if (kind == TreeholeAttachmentKind.IMAGE) "图片" else "音频"
        val name = queryDisplayName(context, uri) ?: fallbackName
        val defaultExtension = if (kind == TreeholeAttachmentKind.IMAGE) "webp" else "m4a"
        val extension = extensionOf(name, defaultExtension)
        val savedUri = try {
            when (kind) {
                TreeholeAttachmentKind.IMAGE -> repositoryFileStorage.saveImage(bytes, extension)
                TreeholeAttachmentKind.AUDIO -> repositoryFileStorage.saveAudio(bytes, extension)
            }
        } catch (e: Exception) {
            return "保存附件失败，请重试"
        }
        val attachment = TreeholeAttachment(
            id = newAttachmentId(kind),
            kind = kind,
            uri = savedUri.toString(),
            name = name,
            sizeBytes = bytes.size.toLong(),
        )
        replaceAttachment(kind, attachment)
        return null
    }

    fun removeImage() {
        viewModelScope.launch { removeAttachment(TreeholeAttachmentKind.IMAGE) }
    }

    fun removeAudio() {
        viewModelScope.launch { removeAttachment(TreeholeAttachmentKind.AUDIO) }
    }

    // ---------- 录音 ----------

    /** 权限被拒：仅提示并引导改用文件选择，不写任何文件。 */
    fun onRecordPermissionDenied() {
        _uiState.update {
            it.copy(attachmentNote = "无法录音：未获得麦克风权限，可改用从文件选择音频")
        }
    }

    fun startRecording() {
        val started = audioRecorder.start()
        _uiState.update {
            if (started) it.copy(recording = true, attachmentNote = null)
            else it.copy(attachmentNote = "无法录音：录音功能当前不可用")
        }
    }

    /** 停止录音并保存为草稿音频附件；过短/超限/失败均不落盘并明确提示。 */
    fun finishRecordingAndAttach() {
        if (!audioRecorder.isRecording) return
        _uiState.update { it.copy(recording = false, attachmentNote = null) }
        viewModelScope.launch {
            val file = audioRecorder.stop()
            if (file == null) {
                _uiState.update {
                    it.copy(attachmentNote = "录音失败（录音太短或设备不可用），未保存任何文件")
                }
                return@launch
            }
            val bytes = runCatching { file.readBytes() }.getOrNull()
            runCatching { file.delete() }
            if (bytes == null || bytes.isEmpty()) {
                _uiState.update {
                    it.copy(attachmentNote = "录音失败（录音太短或设备不可用），未保存任何文件")
                }
                return@launch
            }
            val limitError = TreeholeAttachmentLimits.audioErrorIfAny(bytes.size.toLong())
            if (limitError != null) {
                _uiState.update { it.copy(attachmentNote = limitError) }
                return@launch
            }
            val savedUri = try {
                repositoryFileStorage.saveAudio(bytes, "m4a")
            } catch (e: Exception) {
                _uiState.update { it.copy(attachmentNote = "保存录音失败，请重试") }
                return@launch
            }
            val attachment = TreeholeAttachment(
                id = newAttachmentId(TreeholeAttachmentKind.AUDIO),
                kind = TreeholeAttachmentKind.AUDIO,
                uri = savedUri.toString(),
                name = "录音",
                sizeBytes = bytes.size.toLong(),
            )
            replaceAttachment(TreeholeAttachmentKind.AUDIO, attachment)
        }
    }

    fun cancelRecording() {
        audioRecorder.cancel()
        _uiState.update { it.copy(recording = false, attachmentNote = null) }
    }

    // ---------- 寄出 ----------

    fun submitLetter() {
        val state = _uiState.value
        if (state.submitting) return
        val title = state.title.trim()
        val body = state.body.trim()
        val titleError = when {
            title.isEmpty() -> "请填写标题"
            title.length > 30 -> "标题不能超过 30 字"
            else -> null
        }
        val bodyError = when {
            body.isEmpty() -> "请写下想说的话"
            body.length > 600 -> "正文不能超过 600 字"
            else -> null
        }
        val categoryError = if (state.category == null) "请选择一个分类" else null
        if (titleError != null || bodyError != null || categoryError != null) {
            _uiState.update {
                it.copy(titleError = titleError, bodyError = bodyError, categoryError = categoryError)
            }
            return
        }
        val category = requireNotNull(state.category)
        _uiState.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                pool.submit(
                    title = title,
                    body = body,
                    category = category,
                    paper = state.paper,
                    image = state.image,
                    audio = state.audio,
                )
                _uiState.update {
                    it.copy(
                        submitting = false,
                        tab = TreeholePoolTab.READ,
                        paper = TreeholePaperStyle.PLAIN,
                        category = null,
                        title = "",
                        body = "",
                        image = null,
                        audio = null,
                        infoBanner = SUBMIT_SUCCESS_BANNER,
                    )
                }
                autoClearBanner(SUBMIT_SUCCESS_BANNER)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(submitting = false, infoBanner = "信件提交失败，请稍后重试") }
                autoClearBanner("信件提交失败，请稍后重试")
            }
        }
    }

    // ---------- 拾信展示 ----------

    /** 进入拾信模式且尚未有当前信（或当前信已不在候选）时自动选一封。 */
    fun ensureCurrentLetter(candidates: List<TreeholeLetterLike>) {
        val state = _uiState.value
        if (state.tab != TreeholePoolTab.READ) return
        val current = state.currentLetter
        if (current != null && candidates.any { it.id == current.id }) return
        pickNextLetter(candidates)
    }

    fun changeLetter(candidates: List<TreeholeLetterLike>) {
        pickNextLetter(candidates)
    }

    private fun pickNextLetter(candidates: List<TreeholeLetterLike>) {
        if (candidates.isEmpty()) {
            _uiState.update { it.copy(currentLetter = null, responseMessage = null) }
            return
        }
        val state = _uiState.value
        val currentId = state.currentLetter?.id
        val others = candidates.filter { it.id != currentId }
        val unseen = others.filter { it.id !in readHistory }
        val target = if (unseen.isNotEmpty()) {
            unseen.random()
        } else {
            others.ifEmpty { candidates }.random()
        }
        readHistory += target.id
        while (readHistory.size > 40) {
            readHistory.removeAt(0)
        }
        _uiState.update { it.copy(currentLetter = target, responseMessage = null) }
    }

    /** 轻回应：只产出一次性本地确认文案，不显示任何计数（文案见 kindResponseMessage）。 */
    fun respond(kind: KindResponse) {
        _uiState.update { it.copy(responseMessage = kindResponseMessage(kind)) }
    }

    /** 举报确认后：本地反馈并换一封；拾信池内容保持不变。 */
    fun reportCurrentLetter(candidates: List<TreeholeLetterLike>) {
        _uiState.update { it.copy(infoBanner = "已收到你的反馈") }
        autoClearBanner("已收到你的反馈")
        pickNextLetter(candidates)
    }

    // ---------- 我的信件 ----------

    fun deleteLetter(letter: TreeholeLetterLike) {
        viewModelScope.launch {
            val removed = pool.deleteMine(letter.id)
            if (!removed) return@launch
            deleteAttachmentFilesQuietly(letter)
            _uiState.update { it.copy(infoBanner = "信件已删除") }
            autoClearBanner("信件已删除")
        }
    }

    // ---------- 内部 ----------

    private suspend fun replaceAttachment(
        kind: TreeholeAttachmentKind,
        attachment: TreeholeAttachment,
    ) {
        val old = _uiState.value.let { if (kind == TreeholeAttachmentKind.IMAGE) it.image else it.audio }
        if (old != null) deleteFileQuietly(old)
        _uiState.update { state ->
            if (kind == TreeholeAttachmentKind.IMAGE) state.copy(image = attachment) else state.copy(audio = attachment)
        }
    }

    private suspend fun removeAttachment(kind: TreeholeAttachmentKind) {
        val state = _uiState.value
        val target = if (kind == TreeholeAttachmentKind.IMAGE) state.image else state.audio
        if (target != null) deleteFileQuietly(target)
        _uiState.update { state ->
            if (kind == TreeholeAttachmentKind.IMAGE) state.copy(image = null) else state.copy(audio = null)
        }
    }

    private suspend fun deleteAttachmentFilesQuietly(letter: TreeholeLetterLike) {
        letter.image?.let { deleteFileQuietly(it) }
        letter.audio?.let { deleteFileQuietly(it) }
    }

    private suspend fun deleteFileQuietly(attachment: TreeholeAttachment) {
        try {
            repositoryFileStorage.delete(Uri.parse(attachment.uri))
        } catch (e: Exception) {
            // 清理失败不阻断流程：私有目录残留文件由系统回收即可。
        }
    }

    private fun newAttachmentId(kind: TreeholeAttachmentKind): String {
        attachmentSeq += 1
        return "att-${kind.name.lowercase()}-$attachmentSeq"
    }

    private fun autoClearBanner(text: String) {
        viewModelScope.launch {
            delay(BANNER_AUTO_CLEAR_MILLIS)
            _uiState.update { state ->
                if (state.infoBanner == text) state.copy(infoBanner = null) else state
            }
        }
    }

    override fun onCleared() {
        audioRecorder.cancel()
        super.onCleared()
    }

    private companion object {
        const val SUBMIT_SUCCESS_BANNER = "信件已提交并进入审核，通过后才会与其他来信一同被拾取。"
        const val BANNER_AUTO_CLEAR_MILLIS = 8_000L
    }
}
