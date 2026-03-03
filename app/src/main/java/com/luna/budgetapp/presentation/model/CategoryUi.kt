package com.luna.budgetapp.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flatware
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.luna.budgetapp.ui.icons.BillsIcon
import com.luna.budgetapp.ui.icons.CoffeeIcon
import com.luna.budgetapp.ui.icons.FoodIcon
import com.luna.budgetapp.ui.icons.GroceryIcon
import com.luna.budgetapp.ui.icons.HeartIcon
import com.luna.budgetapp.ui.icons.HouseIcon
import com.luna.budgetapp.ui.icons.MotorcycleIcon
import com.luna.budgetapp.ui.theme.BeverageChartColor
import com.luna.budgetapp.ui.theme.BillsChartColor
import com.luna.budgetapp.ui.theme.CommuteChartColor
import com.luna.budgetapp.ui.theme.DateChartColor
import com.luna.budgetapp.ui.theme.FoodChartColor
import com.luna.budgetapp.ui.theme.GroceryChartColor
import com.luna.budgetapp.ui.theme.HouseChartColor
import com.luna.budgetapp.ui.theme.OthersChartColor
import com.luna.budgetapp.ui.theme.FitnessChartColor
import com.luna.budgetapp.ui.theme.PersonalChartColor

enum class CategoryOptions(
    val icon: ImageVector,
    val chartColor: Color
) {
    FOOD(Icons.Default.Fastfood, FoodChartColor),
    DATE(Icons.Default.FavoriteBorder, DateChartColor),
    BEVERAGE(Icons.Default.Coffee, BeverageChartColor),
    HOUSE(Icons.Default.House, HouseChartColor),
    COMMUTE(Icons.Default.Commute, CommuteChartColor),
    BILLS(BillsIcon, BillsChartColor),
    GROCERY(Icons.Default.LocalGroceryStore, GroceryChartColor),
    FITNESS(Icons.Default.FitnessCenter, FitnessChartColor),
    PERSONAL(Icons.Default.Person, PersonalChartColor),
    OTHERS(Icons.Default.Money, OthersChartColor)
}
