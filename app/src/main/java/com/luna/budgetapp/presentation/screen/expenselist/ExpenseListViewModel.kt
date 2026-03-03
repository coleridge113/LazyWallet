package com.luna.budgetapp.presentation.screen.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.UseCases
import com.luna.budgetapp.presentation.model.ChartData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModel(
    private val useCases: UseCases
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

                useCases.getPagingExpensesByDateRange(selectedCategories, range.start, range.end)
            }
            .cachedIn(viewModelScope)

    init {
        observeActiveProfileAndCategories()
        getCategoryProfileList()
        observeTotalAmount()
        computeChartData()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            Event.ShowCategoryFilterDialog -> showCategoryFilterDialog()
            Event.ShowCalendarForm -> showCalendarForm()
            Event.ResetCategoryFilters -> resetCategoryFilters()
            is Event.DeleteExpense -> deleteExpense(event.expenseId)
            is Event.SelectDateRange -> selectDateRange(event.selectedRange)
            is Event.ShowDeleteConfirmationDialog -> showDeleteConfirmationDialog(event.expenseId)
            is Event.ApplyCategoryFilters -> applyCategoryFilters(event.profileName, event.selectedCategoryMap)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
            is Event.SaveCategoryProfile -> saveCategoryProfile(event.profileName, event.selectedCategoryMap)
        }
    }

    private fun observeTotalAmount() {
        viewModelScope.launch {
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

                    useCases.getTotalAmountByDateRange(
                        start = range.start,
                        end = range.end,
                        categories = selectedCategories
                    )
                }
                .onStart {
                    _uiState.update { it.copy(isExpensesLoading = true) }
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isExpensesLoading = false,
                            error = error.localizedMessage
                        )
                    }
                }
                .collect { totalAmount ->
                    _uiState.update {
                        it.copy(
                            isExpensesLoading = false,
                            error = null,
                            totalAmount = totalAmount
                        )
                    }
                }
        }
    }

    private fun computeChartData() {
        viewModelScope.launch {
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

                    useCases.getCategoryTotalsByDateRange(selectedCategories, range.start, range.end)
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isExpensesLoading = false,
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
                            isExpensesLoading = false,
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
            useCases.deleteExpense(expenseId)
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
                selectedRange = selectedRange,
                dialogState = null
            )
        }
    }

    private fun showCategoryFilterDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                dialogState = 
                    DialogState.CategoryFilterForm(
                        currentState.selectedCategoryMap
                )
            )
        }
    }

    private fun applyCategoryFilters(profileName: String, filters: Map<Category, Boolean>) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategoryMap = filters,
                activeProfile = profileName,
                dialogState = null
            )
        }
    }

    private fun resetCategoryFilters() {
        viewModelScope.launch {
            useCases.setActiveCategoryProfile("All")
        }
    }

    private fun getCategoryProfileList() {
        viewModelScope.launch {
            useCases.getCategoryProfiles().collectLatest { profileList ->
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

            useCases.saveCategoryProfile(filters)

            _uiState.update { currentState ->
                currentState.copy(dialogState = null)
            }
        }
    }

    private fun setActiveCategoryProfile(profileName: String) {
        viewModelScope.launch {
            useCases.setActiveCategoryProfile(profileName)

            _uiState.update { currentState ->
                currentState.copy(dialogState = null)
            }
        }
    }

    private fun observeActiveProfileAndCategories() {
        viewModelScope.launch {
            useCases.getActiveCategoryProfile()
                .flatMapLatest { profile ->
                    useCases.getCategoryProfile(profile)
                        .map { filters ->
                            profile to filters
                        }
                }
                .collectLatest { (profile, filters) ->

                    val categoryMap = filters.associate {
                        it.category to it.isActive
                    }

                    _uiState.update {
                        it.copy(
                            activeProfile = profile,
                            selectedCategoryMap = categoryMap
                        )
                    }
                }
        }
    }
}
