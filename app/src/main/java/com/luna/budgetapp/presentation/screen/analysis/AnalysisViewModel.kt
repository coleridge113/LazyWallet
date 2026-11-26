package com.luna.budgetapp.presentation.screen.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.budgetapp.domain.model.Granularity
import com.luna.budgetapp.domain.model.SpendingHistory
import com.luna.budgetapp.domain.usecase.ExpenseUseCases
import com.luna.budgetapp.domain.usecase.ProfileUseCases
import com.luna.budgetapp.domain.utils.change
import com.luna.budgetapp.domain.utils.periodStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

private const val KEY_GRANULARITY = "granularity"
private const val KEY_SELECTED_START = "selected_start"

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val expenseUseCases: ExpenseUseCases,
    private val profileUseCases: ProfileUseCases,
    @Suppress("unused") private val clock: Clock
) : ViewModel() {

    private val _granularity = MutableStateFlow(
        savedStateHandle.get<String>(KEY_GRANULARITY)
            ?.let { name -> Granularity.entries.firstOrNull { it.name == name } }
            ?: Granularity.DAY
    )

    /** Null means "the current (newest) period". A restored value is snapped to its period start. */
    private val _selectedStart = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_SELECTED_START)
            ?.let { LocalDate.ofEpochDay(it).periodStart(_granularity.value) }
    )

    /** Bumped by [Event.Retry]; each bump restarts the load pipeline. */
    private val _retryTrigger = MutableStateFlow(0)

    private val _categoryProfileState = profileUseCases.observeCategoryProfileState()
        .map { snapshot ->
            CategoryProfileState(
                profileList = snapshot.profileList,
                activeProfile = snapshot.activeProfile,
                selectedCategoryMap = snapshot.categoryMap
            )
        }

    private val _history: Flow<Pair<Granularity, SpendingHistory>> = combine(
        _granularity,
        _categoryProfileState.map { it.activeCategories }.distinctUntilChanged()
    ) { granularity, categories -> granularity to categories }
        .flatMapLatest { (granularity, categories) ->
            expenseUseCases.observeSpendingHistory(categories, granularity)
                .map { granularity to it }
        }

    private val _historyState: Flow<HistoryState> = combine(_history, _selectedStart) { (granularity, history), start ->
        val periods = history.periods
        val index = if (start == null) periods.lastIndex else periods.indexOfFirst { it.start == start }
            .takeIf { it >= 0 } ?: periods.lastIndex
        val selected = periods.getOrNull(index)
        HistoryState(
            granularity = granularity,
            periods = periods,
            selected = selected,
            change = selected?.takeIf { index > 0 }?.let { change(it, periods[index - 1]) },
            today = history.today
        )
    }

    private val _successState = combine(_historyState, _categoryProfileState) { historyState, profileState ->
        historyState to profileState
    }.flatMapLatest { (historyState, profileState) ->
        val selected = historyState.selected
        val expenses = if (selected == null) {
            flowOf(emptyList())
        } else {
            expenseUseCases.getExpensesByDateRange(
                categories = profileState.activeCategories,
                start = selected.start.atStartOfDay(),
                end = selected.end.atTime(LocalTime.MAX)
            )
        }
        expenses.map { list ->
            UiState.Success(
                historyState = historyState,
                expensesState = ExpensesState(filteredExpenses = list),
                categoryProfileState = profileState
            )
        }
    }

    val uiState: StateFlow<UiState> = _retryTrigger
        .flatMapLatest {
            _successState
                .map<UiState.Success, UiState> { it }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(UiState.Error(e.message))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun onEvent(event: Event) {
        when (event) {
            Event.Retry -> _retryTrigger.update { it + 1 }
            is Event.SelectGranularity -> selectGranularity(event.granularity)
            is Event.SelectPeriod -> setSelectedStart(event.start)
            is Event.SelectCategoryProfile -> setActiveCategoryProfile(event.profileName)
        }
    }

    private fun selectGranularity(granularity: Granularity) {
        savedStateHandle[KEY_GRANULARITY] = granularity.name
        setSelectedStart(null)
        _granularity.value = granularity
    }

    private fun setSelectedStart(start: LocalDate?) {
        savedStateHandle[KEY_SELECTED_START] = start?.toEpochDay()
        _selectedStart.value = start
    }

    private fun setActiveCategoryProfile(profileName: String) {
        viewModelScope.launch {
            profileUseCases.setActiveCategoryProfile(profileName)
        }
    }
}
