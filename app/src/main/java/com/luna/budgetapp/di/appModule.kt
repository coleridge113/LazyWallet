package com.luna.budgetapp.di

import androidx.room.Room
import com.luna.budgetapp.data.datastore.AuthLocalDataSource
import com.luna.budgetapp.data.datastore.dataStore
import com.luna.budgetapp.data.local.AppDatabase
import com.luna.budgetapp.data.local.repository.AuthRepositoryImpl
import com.luna.budgetapp.data.local.repository.CategoryFilterRepositoryImpl
import com.luna.budgetapp.data.local.repository.ExpensePresetRepositoryImpl
import com.luna.budgetapp.data.local.repository.ExpenseRepositoryImpl
import com.luna.budgetapp.data.local.migrations.MIGRATION_1_2
import com.luna.budgetapp.data.remote.source.AuthRemoteDataSource
import com.luna.budgetapp.data.utils.PusherManager
import com.luna.budgetapp.domain.repository.AuthRepository
import com.luna.budgetapp.domain.repository.ExpensePresetRepository
import com.luna.budgetapp.domain.repository.ExpenseRepository
import com.luna.budgetapp.domain.repository.CategoryRepository
import com.luna.budgetapp.domain.usecase.UseCases
import com.luna.budgetapp.domain.usecase.auth.GetTokenUseCase
import com.luna.budgetapp.domain.usecase.expense.AddExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.DeleteLatestExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetAllExpensesUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryProfilesUseCase
import com.luna.budgetapp.domain.usecase.expense.GetCategoryTotalsByDateRange
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByCategoryUseCase
import com.luna.budgetapp.domain.usecase.expense.GetTotalAmountByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.GetExpensesByTypeUseCase
import com.luna.budgetapp.domain.usecase.expense.UpdateExpenseUseCase
import com.luna.budgetapp.domain.usecase.expense.GetPagingExpensesByDateRangeUseCase
import com.luna.budgetapp.domain.usecase.expense.SaveCategoryProfileUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.AddExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.DeleteExpensePresetUseCase
import com.luna.budgetapp.domain.usecase.expensepreset.GetAllExpensePresetsUseCase
import com.luna.budgetapp.network.AuthService
import com.luna.budgetapp.network.ExpenseService
import com.luna.budgetapp.network.interceptors.AuthInterceptor
import com.luna.budgetapp.presentation.screen.expensepreset.ExpensePresetViewModel
import com.luna.budgetapp.presentation.screen.expenselist.ExpenseListViewModel
import com.luna.budgetapp.presentation.screen.auth.AuthViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {
    single<HttpLoggingInterceptor>{
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single(named("auth")) {
        val localDataSource = get<AuthLocalDataSource>()

        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .addInterceptor(AuthInterceptor { localDataSource.getJwtToken() })
            .build()
    }

    single(named("public")) {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
    }

    single { get<Retrofit.Builder>().client(get(named("auth"))).build().create(ExpenseService::class.java) }
    single { get<Retrofit.Builder>().client(get(named("public"))).build().create(AuthService::class.java) }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(), 
            AppDatabase::class.java, 
            "budget_db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }
    single { get<AppDatabase>().expenseDao() }
    single { get<AppDatabase>().expensePresetDao() }
    single { get<AppDatabase>().categoryFilterDao() }
    single { androidContext().dataStore }
}

val servicesModule = module {
    singleOf(::PusherManager)
}

val appModule = module {
    includes(networkModule, databaseModule, servicesModule)

    // Repositories
    singleOf(::ExpenseRepositoryImpl) { bind<ExpenseRepository>() }
    singleOf(::ExpensePresetRepositoryImpl) { bind<ExpensePresetRepository>() }
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    singleOf(::CategoryFilterRepositoryImpl) { bind<CategoryRepository>() }
    singleOf(::AuthRemoteDataSource)
    singleOf(::AuthLocalDataSource)

    // UseCases
    factoryOf(::AddExpenseUseCase)
    factoryOf(::DeleteExpenseUseCase)
    factoryOf(::DeleteLatestExpenseUseCase)
    factoryOf(::GetAllExpensesUseCase)
    factoryOf(::GetExpensesByDateRangeUseCase)
    factoryOf(::GetTotalAmountByDateRangeUseCase)
    factoryOf(::GetExpensesByCategoryUseCase)
    factoryOf(::GetExpensesByTypeUseCase)
    factoryOf(::UpdateExpenseUseCase)
    factoryOf(::GetTokenUseCase)
    factoryOf(::GetAllExpensePresetsUseCase)
    factoryOf(::GetCategoryTotalsByDateRange)
    factoryOf(::AddExpensePresetUseCase)
    factoryOf(::GetPagingExpensesByDateRangeUseCase)
    factoryOf(::DeleteExpensePresetUseCase)
    factoryOf(::GetCategoryProfileUseCase)
    factoryOf(::GetCategoryProfilesUseCase)
    factoryOf(::SaveCategoryProfileUseCase)
    factoryOf(::DeleteCategoryProfileUseCase)

    factoryOf(::UseCases)

    // ViewModels
    viewModelOf(::AuthViewModel)
    viewModelOf(::ExpensePresetViewModel)
    viewModelOf(::ExpenseListViewModel)
}
