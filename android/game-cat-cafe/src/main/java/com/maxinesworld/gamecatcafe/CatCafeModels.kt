package com.maxinesworld.gamecatcafe

import com.maxinesworld.gamecatcafe.R

enum class FoodItem(val label: String, val drawable: Int) {
    FISH_SANDWICH("Fish sandwich", R.drawable.food_fish_sandwich),
    FRUIT_CUP("Fruit cup", R.drawable.food_fruit_cup),
    MILK("Milk", R.drawable.food_milk),
    MANGO_SHAKE("Mango shake", R.drawable.food_mango_shake),
    RICE_BOWL("Rice bowl", R.drawable.food_rice_bowl),
    SOUP("Vegetable soup", R.drawable.food_soup),
    BANANA("Banana", R.drawable.food_banana),
    APPLE("Apple", R.drawable.food_apple),
    BREAD("Bread roll", R.drawable.food_bread),
    COOKIE("Cookie", R.drawable.food_cookie)
}

enum class Customer(val label: String, val drawable: Int) {
    CALICO("Callie", R.drawable.customer_calico),
    GRAY_CAT("Ash", R.drawable.customer_gray_cat),
    ASPIN("Duke", R.drawable.customer_aspin),
    RABBIT("Poppy", R.drawable.customer_rabbit),
    OTTER("Tilly", R.drawable.customer_otter),
    OWL("Ollie", R.drawable.customer_owl)
}

data class CafeOrder(val customer: Customer, val items: List<FoodItem>)

data class CatCafeState(
    val order: CafeOrder,
    val tray: List<FoodItem> = emptyList(),
    val roundsCompleted: Int = 0,
    val correctOrders: Int = 0,
    val feedback: String = "Tap the food to fill the tray.",
    val showHint: Boolean = false,
    val roundFinished: Boolean = false
)
