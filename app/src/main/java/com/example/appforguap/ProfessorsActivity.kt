package com.example.appforguap

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.example.appforguap.databinding.ActivityProfessorsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

data class Professor(
    val id: String,
    val name: String,
    val profileUrl: String,
    val imageUrl: String,
    val positions: List<Position> = emptyList(),
    val subjects: List<String> = emptyList()
)
{
    constructor() : this("", "", "", "", emptyList(), emptyList())

}
// Информация для фильтров
data class Position(
    val department: String,
    val title: String,
    val institute: String
)
{
    constructor() : this("", "", "")
}
class ProfessorsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfessorsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    lateinit var searchView: SearchView
    private lateinit var adapter: ProfessorsAdapter
    private var allProfessors: List<Professor> = listOf()
    private var filters: Filters? = null
    private var currentFilters: CurrentFilters = CurrentFilters()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfessorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        checkUser()

        recyclerView = binding.recyclerView
        searchView = binding.searchView
        val searchButton: ImageView = binding.searchButton
        val filterButton: ImageView = binding.filterButton
        val profileImage: ImageView = binding.imageView

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProfessorsAdapter(listOf()) { professor ->
            val intent = Intent(this@ProfessorsActivity, ProfessorPageActivity::class.java).apply {
                putExtra("professor_id", professor.id)
                putExtra("professor_name", professor.name)
                putStringArrayListExtra("professor_subjects", ArrayList(professor.subjects))
                putExtra("professor_image_url", professor.imageUrl)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fetchFilters()
        fetchProfessors()

        setupSearchButton(searchButton)
        setupSearchView()
        setupFilterButton(filterButton)
        setupProfileImage(profileImage)
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun fetchFilters() {
        fetchFiltersFromFirebase(
            onSuccess = { fetchedFilters ->
                filters = fetchedFilters
                Log.d("Firebase", "Фильтры успешно получены: $filters")
            },
            onFailure = { exception ->
                Log.e("Firebase", "Ошибка при получении списка фильтров", exception)
                Toast.makeText(this, "Ошибка при получении списка фильтров: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    fun fetchFiltersFromFirebase(onSuccess: (Filters) -> Unit, onFailure: (Exception) -> Unit) {
        val filtersRef = firebaseDatabase.reference

        filtersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val positions = dataSnapshot.child("position").children.mapNotNull {
                    val text = it.child("text").getValue(String::class.java)
                    val value = it.child("value").getValue(String::class.java)
                    if (text != null && value != null) {
                        FilterOption(value, text)
                    } else {
                        null
                    }
                }.sortedWith(compareBy { it.text.toNaturalSort() }).toMutableList()

                val faculties = dataSnapshot.child("facultyWithChairs").children.mapNotNull {
                    val text = it.child("text").getValue(String::class.java)
                    val value = it.child("value").getValue(String::class.java)
                    if (text != null && value != null) {
                        FilterOption(value, text)
                    } else {
                        null
                    }
                }.sortedWith(compareBy { it.text.toNaturalSort() }).toMutableList()

                val subunits = dataSnapshot.child("subunit").children.mapNotNull {
                    val text = it.child("text").getValue(String::class.java)
                    val value = it.child("value").getValue(String::class.java)
                    if (text != null && value != null) {
                        FilterOption(value, text)
                    } else {
                        null
                    }
                }.sortedWith(compareBy { it.text.toNaturalSort() }).toMutableList()

                positions.add(0, FilterOption("0", "Не выбрано")) // Assuming "0" is a default value for "Не выбрано"
                faculties.add(0, FilterOption("0", "Все"))
                subunits.add(0, FilterOption("0", "Все"))

                val filters = Filters(positions, faculties, subunits)
                onSuccess(filters)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                onFailure(databaseError.toException())
            }
        })
    }

    fun String.toNaturalSort(): String {
        return this.replace(Regex("(\\d+)")) { match ->
            match.value.padStart(10, '0')
        }
    }

    private fun fetchProfessors() {
        firebaseDatabase.getReference("Proffesors").get().addOnSuccessListener { dataSnapshot ->
            allProfessors = dataSnapshot.children.mapNotNull { it.getValue(Professor::class.java) }
            adapter.updateList(allProfessors)
        }.addOnFailureListener { e ->
            Log.e("fetchProfessors", "Ошибка при получении списка преподавателей", e)
            // Handle error (e.g., show toast, retry mechanism, etc.)
        }
    }

    private fun setupSearchButton(searchButton: ImageView) {
        searchButton.setOnClickListener {
            if (searchView.visibility == View.GONE) {
                searchView.visibility = View.VISIBLE
                searchView.isIconified = false
            } else {
                searchView.visibility = View.GONE
            }
        }
    }

    private fun setupSearchView() {
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
        searchView.setOnCloseListener {
            searchView.visibility = View.GONE // Hide the search view when closed
            true
        }
    }

    private fun setupFilterButton(filterButton: ImageView) {
        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
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

    private fun setupProfileImage(profileImage: ImageView) {
        profileImage.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }
}

// Адаптер для RecyclerView
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