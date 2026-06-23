package com.luna.budgetapp.domain.usecase

import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetRemotePresetsUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.UpdateExpensePresetUseCase

data class PresetUseCases(
    val getAllExpensePresets: GetAllExpensePresetsUseCase,
    val addExpensePreset: AddExpensePresetUseCase,
    val deleteExpensePreset: DeleteExpensePresetUseCase,
    val updateExpensePreset: UpdateExpensePresetUseCase,
    val getRemotePresets: GetRemotePresetsUseCase
)
