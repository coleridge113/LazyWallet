package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.time.LocalDateTime
import kotlin.collections.emptyList

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel (
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases
) : ViewModel() {

    private val _expensesState = MutableStateFlow(ExpensesState())
    private val _dateState = MutableStateFlow(DateState())
    private val _categoryProfileState = MutableStateFlow(CategoryProfileState())

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

    val uiState = _successState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    init {
        observeCategoryProfiles()
        observeActiveProfileAndCategories()
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.SelectBar -> setSelectedDate(event.date)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
        }
    }

    private fun observeCategoryProfiles() {
        viewModelScope.launch {
            profileUseCases.getCategoryProfiles()
                .collect { profileList ->
                    _categoryProfileState.update {
                        it.copy(
                            profileList = profileList
                        )
                    }
                }
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

                    _categoryProfileState.update { currentState ->
                        currentState.copy(
                            activeProfile = profile,
                            selectedCategoryMap =
                                categoryMap.ifEmpty { currentState.selectedCategoryMap }
                        )
                    }
                }
        }
    }

    val fooState: StateFlow<ExpensesState> = combine(
        _dateState,
        _categoryProfileState
    ) { dateState, categoryProfileState ->
        val range = dateState.selectedRange.resolve()
        val categories = categoryProfileState.activeCategories

        val expenses = expenseUseCases.getExpensesByDateRange(
            categories = categories,
            start = range.start,
            end = range.end
        )
        ExpensesState(expenses)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpensesState()
        )

    val expenses: StateFlow<List<Expense>> =
        _uiState.map { it.selectedRange to it.selectedCategoryMap }
            .distinctUntilChanged()
            .flatMapLatest { (dateFilter, categoryMap) ->
                val range = dateFilter.resolve()
                expenseUseCases.getExpensesByDateRange(
                    categories = categoryMap.filterValues { it }.keys.map { it.name },
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

    val profileList: StateFlow<List<String>> =
        profileUseCases.getCategoryProfiles()
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

    private fun setActiveCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile(profileName)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <STATE, T> Flow<STATE>.filterDataByState(
        dateFilterSelector: (STATE) -> DateFilter,
        categorySelector: (STATE) -> Map<Category, Boolean>,
        useCase: (categories: List<String>, start: LocalDateTime, end: LocalDateTime) -> Flow<T>
    ): Flow<T> =
        map { state ->
            dateFilterSelector(state) to categorySelector(state)
        }
            .distinctUntilChanged()
            .flatMapLatest { (dateFilter, categoryMap) ->
                val range = dateFilter.resolve()
                val activeCategories =
                    categoryMap
                        .filterValues { it }
                        .keys
                        .map { it.name }

                useCase(activeCategories, range.start, range.end)
            }
}
