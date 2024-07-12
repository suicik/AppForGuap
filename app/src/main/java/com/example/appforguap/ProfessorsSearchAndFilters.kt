// ProfessorsSearchAndFilters.kt

package com.example.appforguap

import androidx.appcompat.app.AlertDialog
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

// Структура для хранения всех получаемых с сервера фильтров
data class FilterOption(val value: String, val text: String)
data class Filters(
    val positions: List<FilterOption>,
    val faculties: List<FilterOption>,
    val subunits: List<FilterOption>
)

// Структура для хранения текущего фильтра
data class CurrentFilters(
    var position: FilterOption? = null,
    var faculty: FilterOption? = null,
    var subunit: FilterOption? = null
)

// Получение списка фильтров (выборка)
fun extractFilters(): Filters {
    val doc: Document = Jsoup.connect("https://pro.guap.ru/professors").get()

    val positions = extractFilterOptions(doc, "#position")
    val faculties = extractFilterOptions(doc, "#facultyWithChairs")
    val subunits = extractFilterOptions(doc, "#subunit")

    return Filters(positions, faculties, subunits)
}
// Парсинг фильтров
fun extractFilterOptions(doc: Document, selector: String): List<FilterOption> {
    val options = mutableListOf<FilterOption>()
    val elements = doc.select("$selector option")

    for (element in elements) {
        val value = element.attr("value").trim()
        val text = element.text().trim()
        options.add(FilterOption(value, text))
    }

    return options
}

object ProfessorsHelper {
    // Изначальная настройка фильтрации
    fun setupFilterDialog(
        activity: AppCompatActivity,
        allProfessors: List<Professor>,
        adapter: ProfessorsAdapter,
        filters: Filters,
        currentFilters: CurrentFilters,
        onApply: (CurrentFilters) -> Unit,
        onReset: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_filters, null)

        val spinnerPositions: Spinner = dialogView.findViewById(R.id.spinnerPositions)
        val spinnerFaculties: Spinner = dialogView.findViewById(R.id.spinnerFaculties)
        val spinnerSubunits: Spinner = dialogView.findViewById(R.id.spinnerSubunits)
        val buttonApplyFilters: Button = dialogView.findViewById(R.id.buttonApplyFilters)
        val resetFiltersButton: Button = dialogView.findViewById(R.id.resetFiltersButton)

        setupSpinner(activity, spinnerPositions, filters.positions.map { it.text })
        setupSpinner(activity, spinnerFaculties, filters.faculties.map { it.text })
        setupSpinner(activity, spinnerSubunits, filters.subunits.map { it.text })

        currentFilters.position?.let { spinnerPositions.setSelection(getIndex(spinnerPositions, it.text)) }
        currentFilters.faculty?.let { spinnerFaculties.setSelection(getIndex(spinnerFaculties, it.text)) }
        currentFilters.subunit?.let { spinnerSubunits.setSelection(getIndex(spinnerSubunits, it.text)) }

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        buttonApplyFilters.setOnClickListener {
            applyFilters(
                activity,
                allProfessors,
                adapter,
                filters,
                currentFilters,
                dialog,
                spinnerPositions,
                spinnerFaculties,
                spinnerSubunits,
                onApply
            )
        }

        resetFiltersButton.setOnClickListener {
            resetFilters(activity, allProfessors, adapter, dialog, onReset)
        }

        dialog.show()
    }
    // Настройка выпадающих список в окне фильтрации
    fun setupSpinner(activity: AppCompatActivity, spinner: Spinner, options: List<String>) {
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
    // Процесс фильтрации
    fun applyFilters(
        activity: AppCompatActivity,
        allProfessors: List<Professor>,
        adapter: ProfessorsAdapter,
        filters: Filters,
        currentFilters: CurrentFilters,
        dialog: AlertDialog,
        spinnerPositions: Spinner,
        spinnerFaculties: Spinner,
        spinnerSubunits: Spinner,
        onApply: (CurrentFilters) -> Unit
    ) {
        val selectedPosition = spinnerPositions.selectedItemPosition
        val selectedFaculty = spinnerFaculties.selectedItemPosition
        val selectedSubunit = spinnerSubunits.selectedItemPosition

        val positionFilter = filters.positions.getOrNull(selectedPosition)
        val facultyFilter = filters.faculties.getOrNull(selectedFaculty)
        val subunitFilter = filters.subunits.getOrNull(selectedSubunit)

        val updatedFilters = CurrentFilters(
            position = positionFilter,
            faculty = facultyFilter,
            subunit = subunitFilter
        )

        onApply(updatedFilters)

        val searchText = (activity as ProfessorsActivity).searchView.query.toString().trim()
        val filteredList = allProfessors.filter { professor ->
            val matchesPosition = positionFilter?.let { filterOption ->
                professor.positions.any { pos -> pos.title == filterOption.text || filterOption.value.toInt() == 0 }
            } ?: true

            val matchesFaculty = facultyFilter?.let { filterOption ->
                professor.positions.any { pos -> pos.institute in filterOption.text || filterOption.value.toInt() == 0 }
            } ?: true

            val matchesSubunit = subunitFilter?.let { filterOption ->
                professor.positions.any { pos -> pos.department in filterOption.text || filterOption.value.toInt() == 0 }
            } ?: true

            matchesPosition && matchesFaculty && matchesSubunit &&
                    (searchText.isBlank() ||
                            professor.name.contains(searchText, ignoreCase = true) ||
                            professor.positions.any { pos -> pos.title.contains(searchText, ignoreCase = true) })
        }

        adapter.updateList(filteredList)
        dialog.dismiss()
    }

    // Сброс фильтров
    fun resetFilters(
        activity: AppCompatActivity,
        allProfessors: List<Professor>,
        adapter: ProfessorsAdapter,
        dialog: AlertDialog,
        onReset: () -> Unit
    ) {
        onReset()

        adapter.updateList(allProfessors)
        dialog.dismiss()
    }
    // Процесс поиска
    fun applySearch(
        adapter: ProfessorsAdapter,
        searchText: String
    ) {
        val filteredList = adapter.professors.filter {
            it.name.contains(searchText, ignoreCase = true) ||
                    it.positions.any { position -> position.title.contains(searchText, ignoreCase = true) }
        }
        adapter.updateList(filteredList)
    }
    // Вспомогательная функция
    fun getIndex(spinner: Spinner, itemName: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == itemName) {
                return i
            }
        }
        return 0
    }
}
