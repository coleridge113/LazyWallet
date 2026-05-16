package com.luna.budgetapp.presentation.screen.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.utils.parseAmountExpression
import com.luna.budgetapp.presentation.model.ChartData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModel(
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _dateState = MutableStateFlow(DateState())
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

    private val _expensesState: Flow<ExpensesState> = combine(
        _dateState,
        _categoryProfileState
    ) { dateState, categoryProfileState ->
        dateState to categoryProfileState
    }
        .flatMapLatest { (dateState, categoryProfileState) ->
            val (start, end) = dateState.dateRange
            expenseUseCases.getTotalAmountByDateRange(
                categories = categoryProfileState.activeCategories,
                start = start,
                end = end
            ).map { amount ->
                ExpensesState(
                    totalAmount = amount
                )
            }
        }

    private val _chartDataState: Flow<ChartDataState> = combine(
        _dateState,
        _categoryProfileState
    ) { dateState, categoryProfileState ->
        dateState.dateRange to categoryProfileState.activeCategories
    }
        .flatMapLatest { (dateRange, activeCategories) ->
            val (start, end) = dateRange
            expenseUseCases.getCategoryTotalsByDateRange(
                categories = activeCategories,
                start = start,
                end
            ).map { data ->
                val chartDataList = data.map { (category, total) ->
                    ChartData(
                        category = category,
                        subtotal = total
                    )
                }

                ChartDataState(
                    chartDataList = chartDataList
                )
            }
        }

    private val _successState = combine(
        _dialogState,
        _dateState,
        _categoryProfileState,
        _chartDataState,
        _expensesState
    ) { dialogState, dateState, categoryProfileState, chartDataState, expensesState ->
        UiState.Success(
            dialogState = dialogState,
            dateState = dateState,
            categoryProfileState = categoryProfileState,
            chartDataState = chartDataState,
            expensesState = expensesState
        )
    }

    val uiState: StateFlow<UiState> = combine(
        _errorState,
        _successState
    ) { error, success ->
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

    private val _navigation = Channel<Navigation>()
    val navigation = _navigation.receiveAsFlow()

    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            Event.ShowCategoryFilterDialog -> showCategoryFilterDialog()
            Event.ShowCalendarForm -> showCalendarForm()
            Event.ResetCategoryFilters -> resetCategoryFilters()
            Event.GotoBarGraph -> gotoAnalysisRoute()
            is Event.ShowExpenseForm -> showExpenseForm(event.selectedExpense)
            is Event.EditExpense -> editExpense(event.expenseId, event.type, event.amount)
            is Event.DeleteExpense -> deleteExpense(event.expenseId)
            is Event.SelectDateRange -> selectDateRange(event.selectedRange)
            is Event.ShowDeleteConfirmationDialog -> showDeleteConfirmationDialog(event.expenseId)
            is Event.ApplyCategoryFilters -> applyCategoryFilters(event.profileName, event.selectedCategoryMap)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
            is Event.SaveCategoryProfile -> saveCategoryProfile(event.profileName, event.selectedCategoryMap)
            is Event.DeleteCategoryProfile -> deleteCategoryProfile(event.profileName)
        }
    }

    val expensesPagingFlow: Flow<PagingData<Expense>> = combine(
        _dateState,
        _categoryProfileState
    ) { dateState, categoryProfileState ->
        dateState to categoryProfileState
    }
        .flatMapLatest { (dateState, categoryProfileState) ->
            val (start, end) = dateState.dateRange
            expenseUseCases.getPagingExpensesByDateRange(
                categories = categoryProfileState.activeCategories,
                start = start,
                end = end
            )
        }

    private fun showDeleteConfirmationDialog(expenseId: Long) {
        _dialogState.update {
            DialogState.ConfirmDeleteExpense(expenseId)
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }

    private fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseUseCases.deleteExpense(expenseId)
            dismissDialog()
        }
    }

    private fun showCalendarForm() {
        _dialogState.update {
            DialogState.CalendarForm
        }
    }

    private fun selectDateRange(selectedFilter: DateFilter) {
        _dateState.update {
            it.copy(
                dateFilter = selectedFilter
            )
        }
        dismissDialog()
    }

    private fun showCategoryFilterDialog() {
        val categoryMap = _categoryProfileState.value.selectedCategoryMap
        _dialogState.update {
            DialogState.CategoryFilterForm(categoryMap)
        }
    }

    private fun applyCategoryFilters(profileName: String, filters: Map<Category, Boolean>) {
        viewModelScope.launch {
            if (profileName == "Custom") saveCategoryProfile(profileName, filters)
            profileUseCases.setActiveCategoryProfile(profileName)
        }
        dismissDialog()
    }

    private fun resetCategoryFilters() {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile("All")
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
            dismissDialog()
        }
    }

    private fun setActiveCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile(profileName)
            dismissDialog()
        }
    }

    private fun deleteCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.deleteCategoryProfile(profileName)
        }
    }

    private fun showExpenseForm(selectedExpense: Expense) {
        _dialogState.update {
            DialogState.ExpenseForm(selectedExpense)
        }
    }

    private fun editExpense(expenseId: Long, type: String, amount: String) {
        viewModelScope.launch {
            expenseUseCases.editExpense(
                id = expenseId,
                type = type,
                amount = parseAmountExpression(amount)
            )
        }

        dismissDialog()
    }

    private fun gotoAnalysisRoute() {
        viewModelScope.launch {
            _navigation.send(Navigation.GotoAnalysisRoute) 
        }
    }
}
