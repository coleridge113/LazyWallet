package com.luna.budgetapp.presentation.screen.budgetdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.usecase.BudgetUseCases

class BudgetDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val budgetUseCases: BudgetUseCases
): ViewModel() {
    val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])
}
