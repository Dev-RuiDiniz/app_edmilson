package com.example.app_edmilson.ui.renderer

import com.example.app_edmilson.data.model.ResolvedTvContent

sealed interface RendererUiState {
    data object Loading : RendererUiState
    data class Success(val content: ResolvedTvContent, val fromCache: Boolean) : RendererUiState
    data class Error(val message: String) : RendererUiState
}

