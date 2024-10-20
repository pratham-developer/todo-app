package com.pratham.todo

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity2 : AppCompatActivity() {

    private lateinit var todoAdapter: TodoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var auth: FirebaseAuth
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        private const val TAG = "MainActivity2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)

        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.rvTodos)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        setupRecyclerView()
        findViewById<FloatingActionButton>(R.id.but).setImageTintList(ColorStateList.valueOf(getColor(R.color.secondary)))
        findViewById<FloatingActionButton>(R.id.but).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showDialog()
        }

        swipeRefreshLayout.setOnRefreshListener {

            fetch()
            swipeRefreshLayout.isRefreshing = false
        }

        setupSidebarButton()
        setupDeleteButton()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter { id, completed ->
            lifecycleScope.launch {
                updateTodoStatus(id, completed)
            }
        }
        recyclerView.adapter = todoAdapter
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity2)
        fetch()
    }

    private fun fetch() {
        lifecycleScope.launch {
            val token = getFirebaseIdToken()
            if (token != null) {
                fetchTodos("Bearer $token")
            } else {
                Log.e(TAG, "Failed to get Firebase ID token")
            }
        }
    }

    private suspend fun getFirebaseIdToken(): String? {
        return try {
            val user = auth.currentUser
            user?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase ID token", e)
            null
        }
    }

    private suspend fun fetchTodos(authToken: String) {
        progressBar.visibility = View.VISIBLE

        try {
            val response = RetrofitInstance.api.getTodos(authToken)
            if (response.isSuccessful && response.body() != null) {
                todoAdapter.todos = response.body()!!
            } else {
                Log.e(TAG, "Response not successful: ${response.code()} - ${response.message()}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException, you might not have internet connection", e)
        } catch (e: HttpException) {
            Log.e(TAG, "HttpException, unexpected response", e)
        } finally {
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private suspend fun createTodo(title: String, authToken: String) {
        val newTodo = todosend(title = title)
        try {
            val response = RetrofitInstance.api.postTodo(authToken, newTodo)
            if (response.isSuccessful) {
                Log.d(TAG, "Todo created: ${response.body()}")
            } else {
                Log.e(TAG, "Failed to create Todo: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Todo", e)
        }
    }

    private fun showDialog() {
        val builder = MaterialAlertDialogBuilder(this, R.style.CustomRoundedDialogTheme)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_forgot, null)
        val editText = dialogLayout.findViewById<TextInputEditText>(R.id.et_title)
        val title = SpannableString("Add Title").apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@MainActivity2, R.color.primary)),
                0, length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        val dialog = builder.setView(dialogLayout)
            .setTitle(title)
            .setPositiveButton("ADD", null)
            .setNegativeButton("CANCEL", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
            findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.tertiary))

            positiveButton.setOnClickListener {
                val titleText = editText.text.toString()
                if (titleText.isNotEmpty()) {
                    addTitle(titleText)
                    dialog.dismiss()
                }
            }
            editText.requestFocus()
        }

        dialog.show()
    }

    private fun addTitle(title: String) {
        lifecycleScope.launch {
            val token = getFirebaseIdToken()
            if (token != null) {
                createTodo(title, "Bearer $token")
                fetch()
                if (todoAdapter.itemCount > 0) {
                    recyclerView.smoothScrollToPosition(todoAdapter.itemCount - 1)
                }
            }
        }
    }

    private suspend fun updateTodoStatus(id: String, completed: Boolean) {
        val token = getFirebaseIdToken()
        if (token != null) {
            val originalTodo = todoAdapter.todos.find { it._id == id }
            val originalCompletedStatus = originalTodo?.completed
            val completedStatus = CompletedStatus(completed)
            try {
                val response = RetrofitInstance.api.updateTodo(id, "Bearer $token", completedStatus)
                if (!response.isSuccessful) {
                    if (originalCompletedStatus != null) {
                        todoAdapter.todos = todoAdapter.todos.map { todo ->
                            if (todo._id == id) todo.copy(completed = originalCompletedStatus) else todo
                        }
                    }
                    Log.e(TAG, "Failed to update Todo: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                if (originalCompletedStatus != null) {
                    todoAdapter.todos = todoAdapter.todos.map { todo ->
                        if (todo._id == id) todo.copy(completed = originalCompletedStatus) else todo
                    }
                }
                Log.e(TAG, "Error updating Todo", e)
            }
        }
    }

    private fun setupDeleteButton() {
        findViewById<ImageView>(R.id.delBut).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            lifecycleScope.launch {
                val token = getFirebaseIdToken()
                if (token != null) {
                    progressBar.visibility = View.VISIBLE // Show progress bar
                    checkCompletedTasksOnServer("Bearer $token")
                }
            }
        }
    }

    private suspend fun checkCompletedTasksOnServer(authToken: String) {
        try {
            val response = RetrofitInstance.api.getTodos(authToken)
            if (response.isSuccessful && response.body() != null) {
                val completedTasks = response.body()!!.filter { it.completed }
                progressBar.visibility = View.GONE // Hide progress bar before showing dialog
                if (completedTasks.isNotEmpty()) {
                    showDeleteConfirmationDialog() // Show dialog to confirm deletion
                } else {
                    showNoCompletedTasksDialog() // Show dialog that no tasks are completed
                }
            } else {
                progressBar.visibility = View.GONE // Hide progress bar if request fails
                Log.e(TAG, "Failed to fetch tasks: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE // Hide progress bar if there's an error
            Log.e(TAG, "Error fetching tasks", e)
        }
    }



    private fun showDeleteConfirmationDialog() {
        val title = SpannableString("Delete Completed Tasks").apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@MainActivity2, R.color.primary)),
                0, length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        MaterialAlertDialogBuilder(this, R.style.CustomRoundedDialogTheme)
            .setTitle(title)
            .setMessage("Are you sure you want to delete all completed tasks?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteCompletedTasks()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
            .apply {
                findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.tertiary))
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
            }
    }

    private fun showNoCompletedTasksDialog() {
        val title = SpannableString("No Completed Tasks").apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@MainActivity2, R.color.primary)),
                0, length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        MaterialAlertDialogBuilder(this, R.style.CustomRoundedDialogTheme)
            .setTitle(title)
            .setMessage("There are no completed tasks to delete.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
            .apply {
                findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.tertiary))
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
            }
    }

    private fun deleteCompletedTasks() {
        lifecycleScope.launch {
            val token = getFirebaseIdToken()
            if (token != null) {
                deleteCompletedTasksFromServer("Bearer $token")
            }
        }
    }

    private suspend fun deleteCompletedTasksFromServer(authToken: String) {
        try {
            val response = RetrofitInstance.api.deleteCompletedTasks(authToken)
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully deleted completed tasks")
                fetch() // Refresh the list of tasks after deletion
            } else {
                Log.e(TAG, "Failed to delete tasks: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting completed tasks", e)
        }
    }

    private fun setupSidebarButton() {
        findViewById<ImageView>(R.id.sidebarButton).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showSignOutConfirmationDialog()
        }
    }

    private fun showSignOutConfirmationDialog() {
        val title = SpannableString("Sign Out").apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@MainActivity2, R.color.primary)),
                0, length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        MaterialAlertDialogBuilder(this, R.style.CustomRoundedDialogTheme)
            .setTitle(title)
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                performSignOut()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
            .apply {
                findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.tertiary))
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
            }
    }

    private fun performSignOut() {
        try {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "exception: ", e)
        }
    }
}