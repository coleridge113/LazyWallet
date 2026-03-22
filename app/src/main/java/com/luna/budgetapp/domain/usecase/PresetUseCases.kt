package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase

data class PresetUseCases(
    val getAllExpensePresets: GetAllExpensePresetsUseCase,
    val addExpensePreset: AddExpensePresetUseCase,
    val deleteExpensePreset: DeleteExpensePresetUseCase,
)
