package com.luna.budgetapp.presentation.screen.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.presentation.model.ChartData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModel(
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    init {
        observeActiveProfileAndCategories()
        getCategoryProfileList()
        computeChartData()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            Event.ShowCategoryFilterDialog -> showCategoryFilterDialog()
            Event.ShowCalendarForm -> showCalendarForm()
            Event.ResetCategoryFilters -> resetCategoryFilters()
            Event.GotoBarGraph -> gotoAnalysisRoute()
            is Event.DeleteExpense -> deleteExpense(event.expenseId)
            is Event.SelectDateRange -> selectDateRange(event.selectedRange)
            is Event.ShowDeleteConfirmationDialog -> showDeleteConfirmationDialog(event.expenseId)
            is Event.ApplyCategoryFilters -> applyCategoryFilters(event.profileName, event.selectedCategoryMap)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
            is Event.SaveCategoryProfile -> saveCategoryProfile(event.profileName, event.selectedCategoryMap)
            is Event.DeleteCategoryProfile -> deleteCategoryProfile(event.profileName)
        }
    }

    val expensesPagingFlow: Flow<PagingData<Expense>> =
        filterDataByState { categories, start, end ->
            expenseUseCases.getPagingExpensesByDateRange(
                categories = categories,
                start = start,
                end = end
            )
        }
            .cachedIn(viewModelScope)

    val totalAmount: StateFlow<Double> =
        filterDataByState { categories, start, end ->
            expenseUseCases.getTotalAmountByDateRange(
                categories = categories,
                start = start,
                end = end
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.0
            )

    private fun computeChartData() {
        viewModelScope.launch {
            _uiState                         
                .map { it.dateFilter to it.selectedCategories }
                .distinctUntilChanged()
                .flatMapLatest { (dateFilter, categoryMap) ->

                    val range = dateFilter.resolve()

                    val selectedCategories =
                        categoryMap
                            .filterValues { it }
                            .keys
                            .map { it.name }

                    expenseUseCases.getCategoryTotalsByDateRange(selectedCategories, range.start, range.end)
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage
                        )
                    }
                }
                .collect { categoryAmounts ->
                    val chartData = categoryAmounts.map { 
                        ChartData(
                            category = it.category,
                            subtotal = it.total
                        )
                    }
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = null,
                            chartDataList = chartData
                        )
                    }
                }
        }
    }


    private fun showDeleteConfirmationDialog(expenseId: Long) {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = DialogState.ConfirmDeleteExpense(expenseId)
            )
        }
    }

    private fun dismissDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = null
            )
        }
    }

    private fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseUseCases.deleteExpense(expenseId)
            _uiState.update { currentState ->
                currentState.copy(
                    dialogState = null
                )
            }
        }
    }

    private fun showCalendarForm() {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = DialogState.CalendarForm
            )
        }
    }

    private fun selectDateRange(selectedRange: DateFilter) {
        _uiState.update { currentState ->
            currentState.copy(
                dateFilter = selectedRange,
                dialogState = null
            )
        }
    }

    private fun showCategoryFilterDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = 
                    DialogState.CategoryFilterForm(
                        currentState.selectedCategories
                )
            )
        }
    }

    private fun applyCategoryFilters(profileName: String, filters: Map<Category, Boolean>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategories = filters,
                dialogState = null
            )
        }

        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile(profileName)
        }
    }

    private fun resetCategoryFilters() {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile("All")
        }
    }

    private fun getCategoryProfileList() {
        viewModelScope.launch {
            profileUseCases.getCategoryProfiles().collectLatest { profileList ->
                _uiState.update { currentState ->
                    currentState.copy(
                        profileList = profileList
                    )
                }
            }
        }
    }
    
    private fun saveCategoryProfile(
        profileName: String,
        categoryMap: Map<Category, Boolean>
    ) {
        viewModelScope.launch {
            val filters = categoryMap.map { (category, isActive) ->
                CategoryFilter(
                    profileName = profileName,
                    category = category,
                    isActive = isActive
                )
            }

            profileUseCases.saveCategoryProfile(filters)

            _uiState.update { currentState ->
                currentState.copy(dialogState = null)
            }
        }
    }

    private fun setActiveCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile(profileName)

            _uiState.update { currentState ->
                currentState.copy(dialogState = null)
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

                    _uiState.update { currentState ->
                        currentState.copy(
                            activeProfile = profile,
                            selectedCategories =
                                categoryMap.ifEmpty { currentState.selectedCategories }
                        )
                    }
                }
        }
    }

    private fun deleteCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.deleteCategoryProfile(profileName)
        }
    }

    private fun gotoAnalysisRoute() {
        viewModelScope.launch {
            _navigation.send(Navigation.GotoAnalysisRoute) 
        }
    }

    private fun <T> filterDataByState(
        useCase: (categories: List<String>, start: LocalDateTime, end: LocalDateTime) -> Flow<T>
    ): Flow<T> {
        return _uiState
            .map { it.dateFilter to it.selectedCategories }
            .distinctUntilChanged()
            .flatMapLatest { (dateFilter, categoryMap) ->
                val range = dateFilter.resolve()
                val activeCategories = categoryMap
                    .filterValues { it }
                    .keys
                    .map { it.name }

                useCase(activeCategories, range.start, range.end)
            }
    }
}
