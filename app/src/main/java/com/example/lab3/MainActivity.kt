package com.example.lab3

import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.room.*
import android.os.Bundle
import kotlinx.coroutines.cancel

class MainActivity : AppCompatActivity() {

    private lateinit var listItems: MutableList<ListItem>
    private lateinit var adapter: MyListAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val logo_text = findViewById<TextView>(R.id.textView)
        val user_input = findViewById<EditText>(R.id.editTextText)
        val add_button = findViewById<Button>(R.id.button)
        val clear_button = findViewById<Button>(R.id.button2)
        val list_view = findViewById<ListView>(R.id.listview)

        listItems = mutableListOf()

        val database = (application as DataBaseInit).database
        val listItemDao = database.listItemDao()

        adapter = MyListAdapter(context = this, listItems, lifecycleScope, database)
        list_view.adapter = adapter

        lifecycleScope.launch {
            listItems.addAll(listItemDao.getAllItems())
            adapter.notifyDataSetChanged()
        }

        add_button.setOnClickListener {
            val taskText = user_input.text.toString()
            if (taskText.isNotBlank()) {
                val newItem = ListItem(task = taskText, status = false)
                lifecycleScope.launch {
                    val newId = listItemDao.insert(newItem)
                    newItem.id = newId
                    listItems.add(newItem)
                    adapter.notifyDataSetChanged()
                    user_input.setText("")
                }
            } else {
                Toast.makeText(this, "Может быть вы хотите что-то сделать?", Toast.LENGTH_SHORT).show()
            }
        }

        clear_button.setOnClickListener {
            clearAll()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun clearAll() {
        lifecycleScope.launch {
            listItems.clear()
            val listItemDao = (application as DataBaseInit).database.listItemDao()
            listItemDao.deleteAll()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }
}


@Entity(tableName = "to_do_list")
data class ListItem(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var task: String,
    var status: Boolean
)


@Dao
interface ListItemDao {

    @Insert
    suspend fun insert(item: ListItem): Long

    @Update
    suspend fun update(listItem: ListItem)

    @Delete
    suspend fun delete(listItem: ListItem)

    @Query("SELECT * FROM to_do_list")
    suspend fun getAllItems(): List<ListItem>

    @Query("DELETE FROM to_do_list")
    suspend fun deleteAll()
}



@Database(
    version = 1,
    entities = [ListItem::class]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listItemDao(): ListItemDao
}
