package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.model.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel (
    private val expenseUseCases: ExpenseUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    val expensesPagingFlow: Flow<PagingData<Expense>> =
        _uiState
            .map { it.selectedRange to it.selectedCategoryMap }
            .distinctUntilChanged()
            .flatMapLatest { (dateFilter, categoryMap) ->
                val range = dateFilter.resolve()
                val selectedCategories =
                    categoryMap
                        .filterValues { it }
                        .keys
                        .map { it.name }

                    expenseUseCases.getPagingExpensesByDateRange(
                        selectedCategories,
                        range.start,
                        range.end
                    )
            }
            .cachedIn(viewModelScope)

    fun onEvent(event: Event) {}

}
