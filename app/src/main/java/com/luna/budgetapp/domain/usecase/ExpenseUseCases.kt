package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetAllExpensesUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryTotalsByDateRange
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByCategoryUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByTypeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetPagingExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.EditExpenseUseCase

data class ExpenseUseCases(
    val addExpense: AddExpenseUseCase,
    val deleteExpense: DeleteExpenseUseCase,
    val deleteLatestExpense: DeleteLatestExpenseUseCase,
    val editExpense: EditExpenseUseCase,
    val getAllExpenses: GetAllExpensesUseCase,
    val getExpensesByCategory: GetExpensesByCategoryUseCase,
    val getExpensesByDateRange: GetExpensesByDateRangeUseCase,
    val getTotalAmountByDateRange: GetTotalAmountByDateRangeUseCase,
    val getCategoryTotalsByDateRange: GetCategoryTotalsByDateRange,
    val getPagingExpensesByDateRange: GetPagingExpensesByDateRangeUseCase,
    val getExpensesByType: GetExpensesByTypeUseCase,
)
