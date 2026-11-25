package com.luna.budgetapp.domain.usecase.budget

import com.luna.budgetapp.domain.model.Budget
import com.luna.budgetapp.domain.model.Category
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.utils.parseAmountExpression
import java.time.Clock
import java.time.LocalDate

/**
 * Builds a [Budget] from raw form input and inserts it (id == 0) or updates it (id != 0).
 */
class SaveBudgetFormUseCase(
    private val saveBudget: SaveBudgetUseCase,
    private val updateBudget: UpdateBudgetUseCase,
    private val clock: Clock
) {
    fun build(
        id: Long,
        name: String,
        amount: String,
        frequency: DateFilter,
        categoryMap: Map<Category, Boolean>
    ): Budget = Budget(
        id = id,
        name = name,
        limit = parseAmountExpression(amount),
        frequency = frequency,
        interactors = categoryMap.filter { it.value }.keys.toList(),
        startDate = LocalDate.now(clock)
    )

    suspend operator fun invoke(
        id: Long,
        name: String,
        amount: String,
        frequency: DateFilter,
        categoryMap: Map<Category, Boolean>
    ) {
        val budget = build(id, name, amount, frequency, categoryMap)
        if (budget.id != 0L) updateBudget(budget) else saveBudget(budget)
    }
}
