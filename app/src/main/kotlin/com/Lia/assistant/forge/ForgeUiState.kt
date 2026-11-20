package com.Lia.assistant.forge

import java.io.File

enum class ForgePhase { IDLE, BUILDING, DONE, FAILED, CANCELLED }

data class ForgeMetrics(
    val chars       : Int   = 0,
    val sections    : Int   = 0,
    val elapsedSec  : Float = 0f,
    val tokensPerSec: Float = 0f,
)

data class ForgeUiState(
    val phase    : ForgePhase   = ForgePhase.IDLE,
    val prompt   : String       = "",
    val isEdit   : Boolean      = false,
    val stage    : ForgeStage   = ForgeStage.READING,
    val progress : Float        = 0f,           // 0..1
    val metrics  : ForgeMetrics = ForgeMetrics(),
    val sections : List<String> = emptyList(),
    val log      : List<String> = emptyList(),
    val logSeq   : Long         = 0L,
    val buildId  : String       = "",
    val file     : File?        = null,
    val error    : String?      = null,
    val partialHtml: String     = "",
) {
    val isBuilding  get() = phase == ForgePhase.BUILDING
    val isDone      get() = phase == ForgePhase.DONE
    val isFailed    get() = phase == ForgePhase.FAILED
    val isCancelled get() = phase == ForgePhase.CANCELLED
    val isIdle      get() = phase == ForgePhase.IDLE
}

sealed class StartResult {
    object Started                          : StartResult()
    data class MissingKey(val msg: String)  : StartResult()
    object Offline                          : StartResult()
    object Busy                             : StartResult()
    object NothingToEdit                    : StartResult()
}
