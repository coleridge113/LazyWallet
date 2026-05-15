package com.luna.budgetapp.data.local.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luna.budgetapp.common.Resource
import com.luna.budgetapp.data.datastore.SettingsDataStore
import com.luna.budgetapp.data.firebase.toFirestoreModel
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao,
    private val api: ExpenseService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsDataStore: SettingsDataStore
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

    override fun getExpensesByCategories(
        categories: List<String>,
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Expense>> =
        dao.getExpensesByCategories(categories, start, end)
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
        val userId = auth.currentUser?.uid
        val remoteId = UUID.randomUUID().toString()

        val entity = expense.toEntity().copy(remoteId = remoteId)
        dao.addExpense(entity)

        if (userId != null) {
            try {
                val firestoreModel = entity.toFirestoreModel()
                firestore.collection("users").document(userId)
                    .collection("expenses").document(remoteId)
                    .set(firestoreModel)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync added expense", e)
            }
        }
    }

    override suspend fun addExpenses(expenses: List<Expense>) {
        dao.addExpenses(expenses.map { it.toEntity() })
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
        // Sync update if needed, but we mainly use editExpenseById
    }

    override suspend fun deleteExpenseById(expenseId: Long) {
        val expense = dao.getExpenseByIdOnce(expenseId)
        val remoteId = expense?.remoteId
        val userId = auth.currentUser?.uid
        
        dao.deleteExpenseById(expenseId)

        if (remoteId != null && userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("expenses").document(remoteId)
                    .delete()
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync deleted expense", e)
            }
        }
    }

    override suspend fun deleteLatestExpense() {
        dao.deleteLatestExpense()
        // Syncing deleteLatest might be complex without the ID, 
        // ideally deleteLatestExpense also returns the deleted entity or its remoteId
    }

    override suspend fun editExpenseById(
        expenseId: Long,
        amount: Double,
        type: String
    ) {
        dao.editExpenseById(expenseId, amount, type)

        val expense = dao.getExpenseByIdOnce(expenseId)
        val remoteId = expense?.remoteId
        val userId = auth.currentUser?.uid

        if (remoteId != null && userId != null && expense != null) {
            try {
                val firestoreModel = expense.toFirestoreModel()
                firestore.collection("users").document(userId)
                    .collection("expenses").document(remoteId)
                    .set(firestoreModel)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync edited expense", e)
            }
        }
    }
}
