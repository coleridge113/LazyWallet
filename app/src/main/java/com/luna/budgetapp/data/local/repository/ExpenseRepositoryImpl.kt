package com.luna.budgetapp.data.local.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.luna.budgetapp.common.Resource
import com.luna.budgetapp.data.local.dao.ExpenseDao
import com.luna.budgetapp.data.mapper.toEntity
import com.luna.budgetapp.data.mapper.toModel
import com.luna.budgetapp.domain.model.Expense
import com.luna.budgetapp.domain.model.CategoryTotalProjection
import com.luna.budgetapp.domain.repository.ExpenseRepository
import com.luna.budgetapp.network.ExpenseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao,
    private val api: ExpenseService
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<PagingData<Expense>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                initialLoadSize = 10,
                prefetchDistance = 1,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.getAllExpenses() }
        ).flow
            .map { pagingData ->
                pagingData.map { it.toModel() }
            }
    }

    override fun getExpensesByCategory(category: String): Flow<Resource<List<Expense>>> {
        return dao.getExpensesByCategory(category)
            .map {
                val resource: Resource<List<Expense>> = Resource.Success(it.map { e -> e.toModel() })
                resource
            }.onStart {
                emit(Resource.Loading)
                try {
                    val remote = api.getExpenseByCategory(category)
                    dao.addExpenses(remote.map { it.toEntity() })
                } catch (e: Exception) { }
            }.flowOn(Dispatchers.IO)
    }

    override fun getExpensesByType(type: String): Flow<Resource<List<Expense>>> = flow { 
        emit(Resource.Loading)
        try {
            val remote = api.getExpenseByType(type)
            dao.addExpenses(remote.map { it.toEntity() })
        } catch (e: Exception) {
            val errorMessage = when(e) {
                is IOException -> "Network error, showing cached data"
                is HttpException -> "Server error, showing cached data"
                else -> "Unknown error occurred"
            }
            emit(Resource.Error(errorMessage))
        }

        emitAll(
            dao.getExpensesByType(type)
            .map { local ->
                Resource.Success(local.map { it.toModel()} )
            }
        )
    }.flowOn(Dispatchers.IO)

    override fun getExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Expense>> =
        dao.getExpensesByDateRange(start, end)
            .map { entities ->
                entities.map { it.toModel() }
            }
            .flowOn(Dispatchers.IO)

    override fun getPagingExpensesByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<PagingData<Expense>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                initialLoadSize = 10,
                prefetchDistance = 1,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { 
                dao.getPagingExpensesByDateRange(start, end)
            }
        ).flow
            .map { pagingData ->
                pagingData.map { it.toModel() }
            }
    }

    override fun getPagingExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<PagingData<Expense>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                initialLoadSize = 10,
                prefetchDistance = 1,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { 
                dao.getPagingExpensesByCategories(categories, start, end)
            }
        ).flow
            .map { pagingData ->
                pagingData.map { it.toModel() }
            }
    }
    
    override fun getTotalAmountByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Double> =
        dao.getTotalAmountByDateRange(start, end)
            .flowOn(Dispatchers.IO)

    override fun getTotalAmountByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Double> =
        dao.getTotalAmountByCategories(categories, start, end)
            .flowOn(Dispatchers.IO)

    override fun getCategoryTotalsByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>> =
        dao.getCategoryTotalsByDateRange(start, end)

    override fun getCategoryTotalsByCategory(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<CategoryTotalProjection>> =
        dao.getCategoryTotalsByCategory(categories, start, end)

    override suspend fun addExpense(expense: Expense) {
        dao.addExpense(expense.toEntity())
    }

    override suspend fun addExpenses(expenses: List<Expense>) {
        dao.addExpenses(expenses.map { it.toEntity() })
    }

    override suspend fun updateExpense(expense: Expense) {
        return dao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpenseById(expenseId: Long) {
        dao.deleteExpenseById(expenseId)
    }

    override suspend fun deleteLatestExpense() {
        dao.deleteLatestExpense()
    }

    override suspend fun editExpenseById(
        expenseId: Long,
        amount: Double,
        type: String
    ) {
        dao.editExpenseById(expenseId, amount, type)
    }
}
