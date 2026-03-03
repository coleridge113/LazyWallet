package com.luna.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.luna.budgetapp.data.local.entity.CategoryFilterEntity

@Dao
interface CategoryFilterDao {

    @Query("SELECT EXISTS(SELECT 1 FROM category_filter LIMIT 1)")
    suspend fun hasAny(): Boolean

    @Query("SELECT DISTINCT profile_name FROM category_filter")
    fun getProfiles(): Flow<List<String>>

    @Query("SELECT * FROM category_filter WHERE profile_name = :profileName")
    fun getProfile(profileName: String): Flow<List<CategoryFilterEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun upsertAll(items: List<CategoryFilterEntity>)

    @Query("DELETE FROM category_filter WHERE profile_name = :profileName")
    suspend fun deleteProfile(profileName: String)
}
