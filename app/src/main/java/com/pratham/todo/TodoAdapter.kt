package com.pratham.todo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class TodoAdapter(private val onTodoCheckedChange: (String, Boolean) -> Unit) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem.title == newItem.title && oldItem.completed == newItem.completed
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)
    var todos: List<Todo>
        get() = differ.currentList
        set(value) { differ.submitList(value) }

    override fun getItemCount() = todos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false) // Assuming your item layout is named item_todo.xml
        return TodoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]
        holder.tvTitle.text = todo.title
        holder.cbDone.isChecked = todo.completed

        // Set up the checkbox click listener
        holder.cbDone.setOnCheckedChangeListener { _, isChecked ->
            // Call the provided callback function to update the todo status
            onTodoCheckedChange(todo._id, isChecked)
        }
    }
}
