package com.example.meal_mission_app.objects

import android.content.Context
import android.content.SharedPreferences
import com.example.meal_mission_app.pages.customer.Cart
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineStorageService {

    private const val PREFERENCES_NAME = "meals_mission_prefs"
    private const val TOKEN_KEY = "token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val USER_ID_KEY = "user_id"
    private const val USER_TYPE = "user_type";
    private const val LOCATION_TASK_ID_KEY = "location_task_id"
    private const val CART_LIST_KEY = "cart"
    private const val RESTAURANT_ID = "restaurant_id"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun saveCredentials(context: Context, token: String?, refreshToken: String?, userId: String?, userType: String?, restaurantId: String?) {
        val editor = getPreferences(context).edit()
        editor.putString(TOKEN_KEY, token)
        editor.putString(REFRESH_TOKEN_KEY, refreshToken)
        editor.putString(USER_ID_KEY, userId)
        editor.putString(USER_TYPE,userType)
        editor.putString(RESTAURANT_ID,restaurantId)
        editor.apply()
    }
    // Save the new cart into a list of carts
    fun saveCart(context: Context, newCart: Cart) {
        val editor = getPreferences(context).edit()

        // Retrieve the current list of carts
        val existingCartsJson = getPreferences(context).getString(CART_LIST_KEY, null)
        val cartList: MutableList<Cart> = if (existingCartsJson != null) {
            Gson().fromJson(existingCartsJson, object : TypeToken<MutableList<Cart>>() {}.type)
        } else {
            mutableListOf()
        }

        // Add the new cart to the list
        cartList.add(newCart)

        // Serialize the updated list back to JSON
        val updatedCartListJson = Gson().toJson(cartList)

        // Save the updated list in SharedPreferences
        editor.putString(CART_LIST_KEY, updatedCartListJson)
        editor.apply()
    }

    // Retrieve the list of carts
    fun getCartList(context: Context): List<Cart> {
        val cartListJson = getPreferences(context).getString(CART_LIST_KEY, null)
        return if (cartListJson != null) {
            Gson().fromJson(cartListJson, object : TypeToken<List<Cart>>() {}.type)
        } else {
            emptyList()  // Return an empty list if no cart data is found
        }
    }
    fun removeCartByOfflineId(context: Context, offlineId: String) {
        val editor = getPreferences(context).edit()

        // Retrieve the current list of carts
        val cartListJson = getPreferences(context).getString(CART_LIST_KEY, null)
        val cartList: MutableList<Cart> = if (cartListJson != null) {
            Gson().fromJson(cartListJson, object : TypeToken<MutableList<Cart>>() {}.type)
        } else {
            mutableListOf()
        }

        // Remove the cart with the specified offlineId
        val iterator = cartList.iterator()
        while (iterator.hasNext()) {
            val cart = iterator.next()
            if (cart.offlineId == offlineId) {
                iterator.remove()
                break
            }
        }

        // Serialize the updated list back to JSON
        val updatedCartListJson = Gson().toJson(cartList)

        // Save the updated list in SharedPreferences
        editor.putString(CART_LIST_KEY, updatedCartListJson)
        editor.apply()
    }

    // Clear the list of carts from SharedPreferences
    fun clearCartList(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(CART_LIST_KEY)  // Remove the entire cart list from SharedPreferences
        editor.apply()
    }


    fun getToken(context: Context): String? {
        return getPreferences(context).getString(TOKEN_KEY, null)
    }
    fun getRestaurantId(context: Context): String? {
        return getPreferences(context).getString(RESTAURANT_ID,null)
    }

    fun getRefreshToken(context: Context): String? {
        return getPreferences(context).getString(REFRESH_TOKEN_KEY, null)
    }

    fun getUserId(context: Context): String? {
        return getPreferences(context).getString(USER_ID_KEY, null)
    }
    fun getUserType(context: Context): String? {
        return getPreferences(context).getString(USER_TYPE,null)
    }

    fun clearCredentials(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }

    // Save location task ID
    fun saveLocationTaskId(context: Context, taskId: String) {
        val editor = getPreferences(context).edit()
        editor.putString(LOCATION_TASK_ID_KEY, taskId)
        editor.apply()
    }

    // Retrieve location task ID
    fun getLocationTaskId(context: Context): String? {
        return getPreferences(context).getString(LOCATION_TASK_ID_KEY, null)
    }

    // Clear location task ID
    fun clearLocationTaskId(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(LOCATION_TASK_ID_KEY)
        editor.apply()
    }
//    fun clearUserCredentials(context: Context) {
//        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        editor.remove(KEY_TOKEN)
//        editor.remove(KEY_USER_ID)
//        editor.remove(KEY_REFRESH_TOKEN)
//        editor.remove(KEY_USER_TYPE)
//        editor.apply()
//    }
}
