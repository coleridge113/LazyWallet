# Implementation Plan - Convert BudgetFrequency to DateFilter

This plan outlines the steps to refactor the `Budget` model to use `DateFilter` instead of the `BudgetFrequency` enum. This involves expanding `DateFilter` with missing frequencies and updating all layers of the application.

## User Review Required

> [!IMPORTANT]
> **Database Migration**: Changing `BudgetFrequency` to `DateFilter` in the Room entity will require a migration if we change how it's serialized. I will update `Converters.kt` to handle the new `DateFilter` implementations.
> **Bi-Weekly Definition**: `BiWeekly` will be implemented as a 14-day period. For now, it will be anchored to the start of the year (weeks 1-2, 3-4, etc.) to match the `Weekly` implementation's calendar-aligned nature.

## Proposed Changes

### Domain Layer

#### [MODIFY] [DateFilter.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/domain/model/DateFilter.kt)
- Add missing frequencies as `data object`s:
    - `BiWeekly`: Every 2 weeks.
    - `Quarterly`: Every 3 months.
    - `BiYearly`: Every 6 months.
    - `Yearly`: Every year.
- Implement `resolve()` for each new frequency.
- Add `fun getFriendlyName(): String` to the `DateFilter` sealed class.
- Add a list of `budgetFrequencies` to the companion object for UI selection.

#### [MODIFY] [Budget.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/domain/model/Budget.kt)
- Change `val frequency: BudgetFrequency` to `val frequency: DateFilter`.
- Delete the `BudgetFrequency` enum.

---

### Data Layer

#### [MODIFY] [BudgetEntity.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/data/local/entity/BudgetEntity.kt)
- Update `frequency` field type to `DateFilter`.

#### [MODIFY] [Converters.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/data/local/Converters.kt)
- Update `fromBudgetFrequency` and `toBudgetFrequency` (rename if appropriate) to handle `DateFilter` serialization.
- Use a string-based format: `"DAILY"`, `"WEEKLY"`, `"BI_WEEKLY"`, `"MONTHLY"`, `"QUARTERLY"`, `"BI_YEARLY"`, `"YEARLY"`, `"CUSTOM|start|end"`.

#### [MODIFY] [SettingsDataStore.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/data/datastore/SettingsDataStore.kt)
- Update to support the new `DateFilter` types in `activeDateFilterFlow` and `setActiveDateFilter`.

---

### Presentation Layer

#### [MODIFY] [BudgetDialog.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/components/BudgetDialog.kt)
- Update `frequencyOptions` to use `DateFilter.budgetFrequencies`.
- Update UI to use `DateFilter.getFriendlyName()`.

#### [MODIFY] [BudgetCard.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/BudgetCard.kt)
- Update previews and any direct frequency references.

#### [MODIFY] [ExpensePresetViewModel.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetViewModel.kt)
- Update `saveBudget` signature and logic.

#### [MODIFY] [DateRangeSelector.kt](file:///home/josem/dev/personal/budget/android/app/src/main/java/com/luna/budgetapp/presentation/screen/components/DateRangeSelector.kt)
- Update to include or handle the new filters if needed.

## Verification Plan

### Manual Verification
- Deploy to device and open the "Create Budget" dialog.
- Verify all frequency options are available and display correctly.
- Save a budget with different frequencies and verify they are stored and displayed correctly in the budget list.
