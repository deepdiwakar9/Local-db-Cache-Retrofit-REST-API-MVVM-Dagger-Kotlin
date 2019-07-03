package com.heske.myrecipes.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import com.heske.myrecipes.AppExecutors
import com.heske.myrecipes.models.Recipe
import com.heske.myrecipes.persistence.RecipeDao
import com.heske.myrecipes.persistence.RecipeDatabase
import com.heske.myrecipes.requests.RecipeApi
import com.heske.myrecipes.requests.responses.RecipeSearchResponse
import com.heske.myrecipes.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/* Copyright (c) 2019 Jill Heske All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
@Singleton
class RecipeRepository @Inject constructor(
    private val appExecutors: AppExecutors,
    private val db: RecipeDatabase,
    private val recipeDao: RecipeDao,
    private val recipeApi: RecipeApi
) {
    private val repoListRateLimit = RateLimiter<String>(10, TimeUnit.MINUTES)

    /**
     * Download one recipe.
     */
    fun searchRecipesApi(recipeId: String): LiveData<Resource<Recipe>> {
        return object : NetworkBoundResource<Recipe, Recipe>(appExecutors) {
            // Called after download completes to insert downloaded
            // data into the database.
            override fun saveCallResult(item: Recipe) {
                recipeDao.insertRecipe(item)
            }

            // true = there's no data, should fetch it from the Network
            override fun shouldFetch(data: Recipe?) = data == null

            override fun loadFromDb() = recipeDao.getRecipe(
                recipe_id = recipeId
            )

            // Make the network call to get the data
            override fun createCall() = recipeApi.getRecipe(API_KEY, recipeId)

        }.asLiveData()
    }

    /**
     * Download a list of recipes
     */
    fun searchRecipesApi(query: String, pageNumber: Int): LiveData<Resource<List<Recipe>>> {
        return object : NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(appExecutors) {
            // Called after download completes to insert downloaded
            // data into the database.
            // RecipeSearchResponse is the deserialized Retrofit response
            // RecipeSearchResult is a database table for storing the list
            // of Recipes along with some metadata about the search.
            override fun saveCallResult(item: RecipeSearchResponse) {
                if (item.recipes == null)
                    return
                Log.d(TAG, "[saveCallResult] Received ${item.recipes.size} recipes ")
//                val recipeIds = item.recipes.map {
//                    it.recipe_id
//                }
//                val recipes = ArrayList<Recipe>(item.recipes.size)
//                for (i in 0..recipes.size-1) {
//
//                }
//                val recipeSearchResult = RecipeSearchResult(
//                    query = query,
//                    recipeIds = recipeIds,
//                    total = item.count,
//                    nextPage = item.nextPage
//                )
//                db.runInTransaction {
//                    recipeDao.insertRecipes(item.recipes)
//                    recipeDao.insertSearchResult(recipeSearchResult)
//                }
            }
//                if (item.recipes != null) { // recipe list will be null if the api key is expired
//                    val recipes = arrayOfNulls<Recipe>(item.recipes.size)
//                    var index = 0
//                    for (rowid in recipeDao.insertRecipes(
//                        item.recipes.toArray(recipes) as Array<Recipe>
//                    )) {
//                        if (rowid == -1) {
//                            Log.d(TAG, "saveCallResult: CONFLICT... This recipe is already in the cache")
//                            // if the recipe already exists... I don't want to set the ingredients or timestamp b/c
//                            // they will be erased
//                            recipeDao.updateRecipe(
//                                recipes[index].recipe_id,
//                                recipes[index].title,
//                                recipes[index].publisher,
//                                recipes[index].image_url,
//                                recipes[index].social_rank
//                            )
//                        }
//                        index++
//                    }
//                }


            // true = there's no data, should fetch it from the Network
            override fun shouldFetch(data: List<Recipe>?) = true

            override fun loadFromDb() = recipeDao.searchRecipes(
                query,
                pageNumber
            )

            // Make the network call to get the data
            override fun createCall() = recipeApi
                .searchRecipe(
                    API_KEY,
                    query,
                    pageNumber.toString()
                );

        }.asLiveData()
    }

}
