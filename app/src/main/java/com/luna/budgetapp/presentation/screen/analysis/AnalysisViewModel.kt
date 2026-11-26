package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
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

    private val _dateState = MutableStateFlow(DateState())
    /** Bumped by [Event.Retry]; each bump restarts the load pipeline. */
    private val _retryTrigger = MutableStateFlow(0)

    private val _categoryProfileState = profileUseCases.observeCategoryProfileState()
        .map { snapshot ->
            CategoryProfileState(
                profileList = snapshot.profileList,
                activeProfile = snapshot.activeProfile,
                selectedCategoryMap = snapshot.categoryMap
            )
        }

    private val _expensesState: Flow<ExpensesState> = combine(
        _dateState,
        _categoryProfileState
    ) { dateState, categoryProfileState ->
        dateState to categoryProfileState
    }
        .flatMapLatest { (dateState, categoryProfileState) ->
            expenseUseCases.getExpensesByDateRange(
                categories = categoryProfileState.activeCategories,
                start = dateState.dateRange.start,
                end = dateState.dateRange.end
            ).map { expenses ->
                val filteredExpenses = expenses.filter {
                    it.date.toLocalDate() == dateState.selectedDate
                }

                ExpensesState(
                    expenses = expenses,
                    filteredExpenses = filteredExpenses
                )
            }
        }

    private val _successState = combine(
        _expensesState,
        _dateState,
        _categoryProfileState
    ) { expensesState, dateState, categoryProfileState ->
        UiState.Success(
            expensesState = expensesState,
            dateState = dateState,
            categoryProfileState = categoryProfileState
        )
    }

    val uiState: StateFlow<UiState> = _retryTrigger
        .flatMapLatest {
            _successState
                .map<UiState.Success, UiState> { it }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(UiState.Error(e.message))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun onEvent(event: Event) {
        when (event) {
            Event.DeselectBar -> setSelectedDate(null)
            Event.Retry -> _retryTrigger.update { it + 1 }
            is Event.SelectBar -> setSelectedDate(event.date)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
        }
    }

    private fun setSelectedDate(date: LocalDate?) {
        _dateState.update {
            it.copy(
                selectedDate = date
            )
        }
    }

    private fun setActiveCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile(profileName)
        }
    }
}
