package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.DateRange
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.usecase.BudgetUseCases
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.utils.parseAmountExpression
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases
): ViewModel() {
    private val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    private val _dateFlow = MutableStateFlow<DateRange?>(null)
    private val _budget = budgetUseCases.getBudgetById(budgetId)
    private val _expenses = combine(
        _budget, 
        _dateFlow
    ) { budget, date ->
        budget to date
    }
        .flatMapLatest { (budget, date) ->
            val (defaultStart, defaultEnd) = budget.frequency.resolve()
            val start = _dateFlow.value?.start
            val end = _dateFlow.value?.end

            expenseUseCases.getExpensesByDateRange(
                categories = budget.interactors.map { it.name },
                start = start ?: defaultStart,
                end = end ?: defaultEnd
            )
        }

    /** Bumped by [Event.Retry]; each bump restarts the load pipeline. */
    private val _retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState> = _retryTrigger
        .flatMapLatest {
            combine(
                _budget,
                _expenses,
                _dialogState
            ) { budget, expenses, dialog ->
                UiState.Success(
                    budget = budget,
                    expenses = expenses,
                    dialog = dialog
                )
            }
                .map<UiState.Success, UiState> { it }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(UiState.Error(e.message))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000.milliseconds),
            initialValue = UiState.Loading
        )

    fun onEvent(event: Event) {
        when (event) {
            Event.DismissDialog -> dismissDialog()
            Event.Retry -> _retryTrigger.update { it + 1 }
            is Event.ConfirmDate -> updateDateRange(event.date)
            is Event.ConfirmYearMonth -> updateDateRange(event.yearMonth)
            is Event.ShowExpenseForm -> showExpenseForm(event.selectedExpense)
            is Event.DeleteExpense -> deleteExpense(event.expenseId)
            is Event.ShowDeleteConfirmationDialog -> 
                showDeleteConfirmationDialog(event.expenseId)
            is Event.EditExpense -> 
                editExpense(event.expenseId, event.type, event.amount, event.date)

        }
    }

    private fun updateDateRange(date: LocalDate) {
        _dateFlow.update {
            DateRange(
                start = date.atStartOfDay(),
                end = date.atTime(LocalTime.MAX)
            )
        }
        dismissDialog()
    }

    private fun updateDateRange(yearMonth: YearMonth) {
        _dateFlow.update {
            DateRange(
                start = yearMonth.atDay(1).atStartOfDay(),
                end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)
            )
        }
        dismissDialog()
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

    private fun showDeleteConfirmationDialog(expenseId: Long) {
        _dialogState.update { DialogState.DeleteConfirmation(expenseId) }
    }

    private fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseUseCases.deleteExpense(expenseId)
            dismissDialog()
        }
    }

    private fun dismissDialog() {
        _dialogState.update { null }
    }
}
