package com.hotspottv.ui.renderer

import com.hotspottv.data.model.ResolvedTvContent

sealed interface RendererUiState {
    data object Loading : RendererUiState
    data class Success(val content: ResolvedTvContent, val fromCache: Boolean) : RendererUiState
    data class Error(val message: String, val cause: Throwable? = null) : RendererUiState
}
