package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.auth.GetTokenUseCase
import com.luna.budgetapp.domain.usecase.category.DeleteCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.GetCategoryProfilesUseCase
import com.luna.budgetapp.domain.usecase.category.InitializeCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.category.SaveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetAllExpensesUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByCategoryUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByTypeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryTotalsByDateRange
import com.luna.budgetapp.domain.usecase.expense.GetPagingExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase

data class UseCases(
    val getToken: GetTokenUseCase,
    val addExpense: AddExpenseUseCase,
    val deleteExpense: DeleteExpenseUseCase,
    val deleteLatestExpense: DeleteLatestExpenseUseCase,
    val getAllExpenses: GetAllExpensesUseCase,
    val getExpensesByCategory: GetExpensesByCategoryUseCase,
    val getExpensesByDateRange: GetExpensesByDateRangeUseCase,
    val getTotalAmountByDateRange: GetTotalAmountByDateRangeUseCase,
    val getCategoryTotalsByDateRange: GetCategoryTotalsByDateRange,
    val getPagingExpensesByDateRange: GetPagingExpensesByDateRangeUseCase,
    val getExpensesByType: GetExpensesByTypeUseCase,
    val getAllExpensePresets: GetAllExpensePresetsUseCase,
    val addExpensePreset: AddExpensePresetUseCase,
    val deleteExpensePreset: DeleteExpensePresetUseCase,
    val getCategoryProfile: GetCategoryProfileUseCase,
    val getCategoryProfiles: GetCategoryProfilesUseCase,
    val saveCategoryProfile: SaveCategoryProfileUseCase,
    val deleteCategoryProfile: DeleteCategoryProfileUseCase,
    val initializeCategoryProfile: InitializeCategoryProfileUseCase
)
