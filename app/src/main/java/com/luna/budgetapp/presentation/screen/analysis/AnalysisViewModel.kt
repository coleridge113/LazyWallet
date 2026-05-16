package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val _errorState = MutableStateFlow<String?>(null)

    private val _categoryProfileState = profileUseCases.getActiveCategoryProfile()
        .flatMapLatest { activeProfile ->
            profileUseCases.getCategoryProfile(activeProfile)
                .map { filters ->
                    val categoryMap = filters.associate { it.category to it.isActive }
                    activeProfile to categoryMap
                }
        }
        .combine(profileUseCases.getCategoryProfiles()) { (activeProfile, categoryMap), profileList ->
            CategoryProfileState(
                profileList = profileList,
                activeProfile = activeProfile,
                selectedCategoryMap = categoryMap
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryProfileState()
        )

    private val _expensesState: StateFlow<ExpensesState> = combine(
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpensesState()
        )

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

    val uiState: StateFlow<UiState> = combine(
        _successState,
        _errorState
    ) { success, error ->
        if (error != null) {
            UiState.Error(error)
        } else {
            success
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun onEvent(event: Event) {
        when (event) {
            is Event.SelectBar -> setSelectedDate(event.date)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
        }
    }

    private fun setSelectedDate(date: LocalDate) {
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
