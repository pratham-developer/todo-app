package com.pratham.todo

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val api: TodoApi by lazy {
        createRetrofit()
    }

    private fun createRetrofit(): TodoApi {
        return Retrofit.Builder()
            .baseUrl("https://todo-backend-self.vercel.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TodoApi::class.java)
    }
}
