package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel (
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeActiveProfileAndCategories()
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.SelectBar -> setSelectedDate(event.date)
        }
    }
    
    private fun observeActiveProfileAndCategories() {
        viewModelScope.launch {
            profileUseCases.getActiveCategoryProfile()
                .flatMapLatest { profile ->
                    profileUseCases.getCategoryProfile(profile)
                        .map { filters ->
                            profile to filters
                        }
                }
                .collectLatest { (profile, filters) ->

                    val categoryMap = filters.associate {
                        it.category to it.isActive
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            activeProfile = profile,
                            selectedCategoryMap =
                                categoryMap.ifEmpty { currentState.selectedCategoryMap }
                        )
                    }
                }
        }
    }

    val expenses: StateFlow<List<Expense>> =
        _uiState
            .map { it.selectedRange }
            .distinctUntilChanged()
            .flatMapLatest { dateFilter ->
                val range = dateFilter.resolve()
                expenseUseCases.getExpensesByDateRange(
                    start = range.start,
                    end = range.end
                )
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )

    val filteredExpenses: StateFlow<List<Expense>> =
        combine(
            expenses,
            _uiState.map { it.selectedDate }.distinctUntilChanged()
        ) { expenseList, selectedDate ->

            expenseList.filter { expense ->
                expense.date.toLocalDate() == selectedDate
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private fun setSelectedDate(date: LocalDate) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedDate = date
            )
        }
    }
}
