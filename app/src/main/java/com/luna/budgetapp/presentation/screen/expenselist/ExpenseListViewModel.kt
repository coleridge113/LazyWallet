package com.luna.budgetapp.presentation.screen.expenselist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.luna.budgetapp.common.Constants.CUSTOM
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.CategoryFilter
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.utils.parseAmountExpression
import com.luna.budgetapp.domain.utils.toCategoryMap
import com.luna.budgetapp.presentation.model.ChartData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExpenseListViewModel(
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _categoryPreview = MutableStateFlow<CategoryProfilePreview?>(null)
    private val _dateState = MutableStateFlow(DateState())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _categoryProfileState = profileUseCases.observeCategoryProfileState()
        .map { snapshot ->
            CategoryProfileState(
                profileList = snapshot.profileList,
                activeProfile = snapshot.activeProfile,
                selectedCategoryMap = snapshot.categoryMap
            )
        }
        .catch { e ->
            if (e is CancellationException) throw e
            _errorState.value = e.message ?: "Something went wrong"
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
        _categoryPreview,
        _dateState,
        _categoryProfileState,
        _chartDataState,
        _expensesState
    ) { values ->
        val dialogState = values[0] as DialogState?
        val categoryPreview = values[1] as CategoryProfilePreview?
        val dateState = values[2] as DateState
        val categoryProfileState = values[3] as CategoryProfileState
        val chartDataState = values[4] as ChartDataState
        val expensesState = values[5] as ExpensesState
        UiState.Success(
            dialogState = dialogState,
            categoryPreview = categoryPreview,
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
        .catch { e ->
            if (e is CancellationException) throw e
            emit(UiState.Error(e.message ?: "Something went wrong"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            Event.ResetCategoryFilters -> resetCategoryFilters()
            is Event.ShowExpenseForm -> showExpenseForm(event.selectedExpense)
            is Event.EditExpense -> editExpense(event.expenseId, event.type, event.amount, event.date)
            is Event.DeleteExpense -> deleteExpense(event.expenseId)
            is Event.SearchQueryChanged -> _searchQuery.value = event.query
            is Event.SelectDateRange -> selectDateRange(event.selectedRange)
            is Event.ShowDeleteConfirmationDialog -> showDeleteConfirmationDialog(event.expenseId)
            is Event.ApplyCategoryFilters -> applyCategoryFilters(event.profileName, event.selectedCategoryMap)
            is Event.PreviewCategoryProfile -> previewCategoryProfile(event.profileName)
            Event.ClearCategoryPreview -> _categoryPreview.value = null
            is Event.SaveCategoryProfile -> saveCategoryProfile(event.profileName, event.selectedCategoryMap)
            is Event.DeleteCategoryProfile -> deleteCategoryProfile(event.profileName)
        }
    }

    val expensesPagingFlow: Flow<PagingData<Expense>> = combine(
        _dateState,
        _categoryProfileState,
        _searchQuery.debounce(300).distinctUntilChanged()
    ) { dateState, categoryProfileState, query ->
        Triple(dateState, categoryProfileState, query)
    }
        .flatMapLatest { (dateState, categoryProfileState, query) ->
            val (start, end) = dateState.dateRange
            expenseUseCases.getPagingExpensesByDateRange(
                categories = categoryProfileState.activeCategories,
                start = start,
                end = end,
                query = query
            )
        }
        .cachedIn(viewModelScope)

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

    private fun selectDateRange(selectedFilter: DateFilter) {
        _dateState.update {
            it.copy(
                dateFilter = selectedFilter
            )
        }
        dismissDialog()
    }

    private fun applyCategoryFilters(profileName: String, filters: Map<Category, Boolean>) {
        viewModelScope.launch {
            if (profileName == CUSTOM) saveCategoryProfileInternal(profileName, filters)
            profileUseCases.setActiveCategoryProfile(profileName)
        }
        _categoryPreview.value = null
        dismissDialog()
    }

    private fun previewCategoryProfile(profileName: String) {
        viewModelScope.launch {
            val filters = profileUseCases.getCategoryProfile(profileName).first()
            _categoryPreview.value = CategoryProfilePreview(profileName, filters.toCategoryMap())
        }
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
            saveCategoryProfileInternal(profileName, categoryMap)
            dismissDialog()
        }
    }

    private suspend fun saveCategoryProfileInternal(
        profileName: String,
        categoryMap: Map<Category, Boolean>
    ) {
        val filters = categoryMap.map { (category, isActive) ->
            CategoryFilter(
                profileName = profileName,
                category = category,
                isActive = isActive
            )
        }

        profileUseCases.saveCategoryProfile(filters)
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

    private fun editExpense(expenseId: Long, type: String, amount: String, date: LocalDateTime) {
        viewModelScope.launch {
            expenseUseCases.editExpense(
                id = expenseId,
                type = type,
                amount = parseAmountExpression(amount),
                date = date
            )
        }

        dismissDialog()
    }
}
