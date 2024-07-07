package com.example.appforguap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView

class ProfessorsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    lateinit var searchView: SearchView
    private lateinit var adapter: ProfessorsAdapter
    private var allProfessors: List<Professor> = listOf()
    private var filters: Filters? = null
    private var currentFilters: CurrentFilters = CurrentFilters()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professors)

        enableEdgeToEdge()

        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)
        val searchButton: ImageView = findViewById(R.id.searchButton)
        val filterButton: ImageView = findViewById(R.id.filterButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        GlobalScope.launch(Dispatchers.IO) {
            allProfessors = parseAllProfessors()
            filters = extractFilters()

            launch(Dispatchers.Main) {
                adapter = ProfessorsAdapter(allProfessors) { professor ->
                    val intent = Intent(this@ProfessorsActivity, ProfessorPageActivity::class.java).apply {
                        putExtra("professor_name", professor.name)
                        putStringArrayListExtra("professor_subjects", ArrayList(professor.subjects))
                        putExtra("professor_image_url", professor.imageUrl)
                    }
                    startActivity(intent)
                }
                recyclerView.adapter = adapter
            }
        }

        setupSearchButton(searchButton)
        setupSearchView()
        setupFilterButton(filterButton)
    }

    // Настрока кнопки поиска
    fun setupSearchButton(searchButton: ImageView) {
        searchButton.setOnClickListener {
            if (searchView.visibility == View.GONE) {
                searchView.visibility = View.VISIBLE
                searchView.isIconified = false
            } else {
                searchView.visibility = View.GONE
            }
        }
    }

    // Настройка окна ввода для поиска
    fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    val filteredList = allProfessors.filter { professor ->
                        professor.name.contains(newText, ignoreCase = true) ||
                                professor.positions.any { position ->
                                    position.title.contains(newText, ignoreCase = true)
                                }
                    }.filter { professor ->
                        val matchesPosition = currentFilters.position?.let { filterOption ->
                            professor.positions.any { pos -> pos.title in filterOption.text || filterOption.value.toInt() == 0 }
                        } ?: true

                        val matchesFaculty = currentFilters.faculty?.let { filterOption ->
                            professor.positions.any { pos -> pos.institute in filterOption.text || filterOption.value.toInt() == 0 }
                        } ?: true

                        val matchesSubunit = currentFilters.subunit?.let { filterOption ->
                            professor.positions.any { pos -> pos.department in filterOption.text || filterOption.value.toInt() == 0 }
                        } ?: true

                        matchesPosition && matchesFaculty && matchesSubunit
                    }
                    adapter.updateList(filteredList)
                }
                return true
            }
        })
    }

    // Настройка кнопки для открытия окна фильтрации
    fun setupFilterButton(filterButton: ImageView) {
        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    // Показ окна фильтрации
    fun showFilterDialog() {
        filters?.let {
            ProfessorsHelper.setupFilterDialog(
                this,
                allProfessors,
                adapter,
                it,
                currentFilters,
                onApply = { updatedFilters ->
                    currentFilters = updatedFilters
                },
                onReset = {
                    currentFilters = CurrentFilters()
                }
            )
        }
    }
}

class ProfessorsAdapter(
    var professors: List<Professor>,
    private val itemClickListener: (Professor) -> Unit
) : RecyclerView.Adapter<ProfessorsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_professor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val professor = professors[position]

        val formattedPositions = professor.positions.joinToString("\n") {
            "${it.title}, ${it.department}"
        }

        holder.nameTextView.text = professor.name
        holder.positionsTextView.text = formattedPositions
        loadImageWithRotation(holder.imageView.context, professor.imageUrl, holder.imageView)

        holder.itemView.setOnClickListener {
            itemClickListener(professor)
        }
    }

    override fun getItemCount(): Int {
        return professors.size
    }

    fun updateList(newProfessors: List<Professor>) {
        professors = newProfessors
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val positionsTextView: TextView = itemView.findViewById(R.id.positionsTextView)
    }
}
