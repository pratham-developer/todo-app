package com.pratham.todo

import retrofit2.http.GET
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TodoApi {
    @GET("/apikey/tasks")
    suspend fun getTodos(
        @Header("Authorization") authToken: String
    ): Response<List<Todo>>

    @POST("/apikey/tasks")
    suspend fun postTodo(
        @Header("Authorization") authToken: String,
        @Body todo: todosend // Body parameter for the new todo item
    ): Response<Todo>

    @PATCH("/apikey/tasks/{id}")
    suspend fun updateTodo(
        @Path("id") id: String, // ID of the task to be updated
        @Header("Authorization") authToken: String,
        @Body completedStatus: CompletedStatus
    ): Response<Todo>

    @DELETE("/apikey/tasks")
    suspend fun deleteCompletedTasks(
        @Header("Authorization") authToken: String
    ): Response<Void> // Use Void if no content is returned
}


data class todosend(
    val title : String
)
data class CompletedStatus(val completed: Boolean)