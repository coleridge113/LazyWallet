package com.luna.budgetapp.presentation.screen.expenselist

import com.luna.budgetapp.presentation.screen.components.LoadingContent
import com.luna.budgetapp.presentation.screen.components.ErrorContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.luna.budgetapp.R
import com.luna.budgetapp.domain.model.DateFilter
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.presentation.screen.components.CategoryFilterDialog
import com.luna.budgetapp.presentation.screen.components.ConfirmationDialog
import com.luna.budgetapp.presentation.screen.components.DateRangePickerDialog
import com.luna.budgetapp.presentation.screen.components.DateRangeSelectorDropdown
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpandableSearchFab
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseChart
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseForm
import com.luna.budgetapp.presentation.screen.expenselist.components.ExpenseTable
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseListRoute(
    viewModel: ExpenseListViewModel,
    onOpenAnalysis: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val expenses = viewModel.expensesPagingFlow.collectAsLazyPagingItems()
    // Picker visibility is pure UI state; the ViewModel only receives the picked result.
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showCategoryFilter by rememberSaveable { mutableStateOf(false) }

    when (val state = uiState) {
        is UiState.Loading -> LoadingContent()
        is UiState.Error -> ErrorContent(
            onRetry = { viewModel.onEvent(Event.Retry) }
        )
        is UiState.Success -> {
            val imeVisible = WindowInsets.isImeVisible
            val fabBottomPadding by animateDpAsState(
                targetValue = if (imeVisible) 16.dp else 72.dp,
                label = "searchFabBottomPadding"
            )
            val focusManager = LocalFocusManager.current
            // Plain holders (not state): only read inside the pointer handler.
            val rootCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
            val fabCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { rootCoords[0] = it }
                    .pointerInput(Unit) {
                        // Observe taps without consuming so children still get them.
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            val root = rootCoords[0]
                            val fab = fabCoords[0]
                            val insideFab = if (
                                root != null && root.isAttached && fab != null && fab.isAttached
                            ) {
                                root.localBoundingBoxOf(fab).contains(down.position)
                            } else {
                                false
                            }
                            if (!insideFab) focusManager.clearFocus()
                        }
                    }
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    modifier = Modifier,
                    title = {},
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    actions = {
                        DateRangeSelectorDropdown(
                            selected = state.dateState.dateFilter,
                            onSelectedChange = {
                                when (it) {
                                    DateFilter.Daily,
                                    DateFilter.Weekly,
                                    DateFilter.Monthly -> viewModel.onEvent(Event.SelectDateRange(it))
                                    else -> showCalendar = true
                                }
                            }
                        )
                        IconButton(onClick = onOpenAnalysis) {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = stringResource(R.string.cd_open_analysis)
                            )
                        }
                    }
                )

                ExpenseListContent(
                    modifier = Modifier,
                    uiState = state,
                    onEvent = viewModel::onEvent,
                    totalAmount = state.expensesState.totalAmount,
                    expenses = expenses,
                    searchQuery = searchQuery,
                    showCalendar = showCalendar,
                    onDismissCalendar = { showCalendar = false },
                    showCategoryFilter = showCategoryFilter,
                    onShowCategoryFilter = { showCategoryFilter = true },
                    onDismissCategoryFilter = { showCategoryFilter = false }
                )
            }
            ExpandableSearchFab(
                query = searchQuery,
                onQueryChange = { viewModel.onEvent(Event.SearchQueryChanged(it)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, end = 16.dp, bottom = fabBottomPadding)
                    .imePadding(),
                onPositioned = { fabCoords[0] = it }
            )
            }
        }
    }
}

@Composable
fun ExpenseListContent(
    modifier: Modifier,
    uiState: UiState.Success,
    onEvent: (Event) -> Unit,
    totalAmount: Long,
    expenses: LazyPagingItems<Expense>,
    searchQuery: String,
    showCalendar: Boolean,
    onDismissCalendar: () -> Unit,
    showCategoryFilter: Boolean,
    onShowCategoryFilter: () -> Unit,
    onDismissCategoryFilter: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        ExpenseChart(
            chartDataList = uiState.chartDataState.chartDataList,
            totalAmount = totalAmount,
            showDialog = onShowCategoryFilter,
            onClickCenter = { onEvent(Event.ResetCategoryFilters) }
        )
        Spacer(modifier = Modifier.height(48.dp))
        when {
            expenses.itemCount <= 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(R.string.no_expenses_filtered_range)
                        } else {
                            stringResource(R.string.no_expenses_match_query, searchQuery.trim())
                        },
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            }
            else -> {
                ExpenseTable(
                    modifier = Modifier,
                    expenses = expenses,
                    onEdit = { onEvent(Event.ShowExpenseForm(it)) },
                    onDelete = { onEvent(Event.ShowDeleteConfirmationDialog(it.id!!)) },
                    contentPadding = PaddingValues(bottom = 80.dp)
                )
            }
        }

        if (showCalendar) {
            DateRangePickerDialog(
                onDismiss = onDismissCalendar,
                onConfirm = { start, end ->
                    if (start != null) {
                        onEvent(Event.SelectDateRange(DateFilter.Custom(start, end)))
                    }
                    onDismissCalendar()
                }
            )
        }
        if (showCategoryFilter) {
            CategoryFilterDialog(
                selectedCategoryMap = uiState.categoryPreview?.categoryMap
                    ?: uiState.categoryProfileState.selectedCategoryMap,
                selectedProfile = uiState.categoryPreview?.profileName
                    ?: uiState.categoryProfileState.activeProfile,
                profileList = uiState.categoryProfileState.profileList,
                onDismiss = {
                    onEvent(Event.ClearCategoryPreview)
                    onDismissCategoryFilter()
                },
                onApply = { profileName, filters ->
                    onEvent(Event.ApplyCategoryFilters(profileName, filters))
                    onDismissCategoryFilter()
                },
                onSelectedChange = { profileName ->
                    onEvent(Event.PreviewCategoryProfile(profileName))
                },
                onSave = { newProfileName, filters ->
                    onEvent(Event.SaveCategoryProfile(newProfileName, filters))
                }
            )
        }

        when (val dialog = uiState.dialogState) {
            is DialogState.ConfirmDeleteExpense ->
                ConfirmationDialog(
                    message = stringResource(R.string.confirm_delete_expense),
                    confirmText = stringResource(R.string.btn_delete),
                    isDestructive = true,
                    onDismiss = { onEvent(Event.DismissDialog) },
                    onConfirm = { onEvent(Event.DeleteExpense(dialog.expenseId)) }
                )

            is DialogState.ExpenseForm ->
                ExpenseForm(
                    selectedExpense = dialog.selectedExpense,
                    onDismissRequest = { onEvent(Event.DismissDialog) },
                    onConfirm = { expenseId, type, amount, date ->
                        onEvent(Event.EditExpense(expenseId, type, amount, date))
                    },
                    isSaving = dialog.isSaving
                )

            else -> {}
        }
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO
)
@Composable
private fun ExpenseListContentPreviewLight() {
    val successState = UiState.Success()

    LazyWalletTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            // Preview simplified to avoid paging data issues
            Text("Preview of ExpenseListContent")
        }
    }
}
