package com.hotspottv.ui.renderer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.hotspottv.data.model.TvRenderContent
import com.hotspottv.data.repository.TvContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RendererViewModel(
    private val repository: TvContentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<RendererUiState>(RendererUiState.Loading)
    val uiState: StateFlow<RendererUiState> = _uiState.asStateFlow()

    private var currentCode: String? = null

    fun load(code: String) {
        currentCode = code
        _uiState.value = RendererUiState.Loading

        viewModelScope.launch {
            val result = repository.fetchContent(code)
            _uiState.value = result.fold(
                onSuccess = { RendererUiState.Success(it.content, it.fromCache) },
                onFailure = { RendererUiState.Error(it.message ?: "Falha ao carregar conteúdo") }
            )
        }
    }

    fun reload() {
        currentCode?.let(::load)
    }

    fun reportDisplay(content: TvRenderContent) {
        val code = currentCode ?: return
        viewModelScope.launch {
            repository.registerDisplay(code, content)
                .onFailure { error ->
                    Log.w(TAG, "Falha ao registrar exibicao da propaganda", error)
                }
        }
    }

    companion object {
        private const val TAG = "RendererViewModel"
    }
}

class RendererViewModelFactory(
    private val repository: TvContentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RendererViewModel::class.java)) {
            return RendererViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
