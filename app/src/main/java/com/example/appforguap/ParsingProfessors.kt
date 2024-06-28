package com.example.appforguap

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException

// Информация о преподавателе
data class Professor(
    val name: String,
    val profileUrl: String,
    val imageUrl: String,
    val positions: List<Position> = emptyList()
)
// Информация для фильтров
data class Position(
    val department: String,
    val title: String,
    val institute: String
)
// Фильтры
data class FilterOption(val value: String, val text: String)
data class Filters(
    val positions: List<FilterOption>,
    val faculties: List<FilterOption>,
    val subunits: List<FilterOption>
)
// Парсинг страниц
suspend fun parseAllProfessors(): List<Professor> = coroutineScope {
    val baseUrl = "https://pro.guap.ru/professors?page="
    val allProfessors = mutableListOf<Professor>()
    var page = 1
    var isMorePages = true

    while (isMorePages) {
        val url = "$baseUrl$page"
        println("Parsing page: $page")

        try {
            val doc: Document = Jsoup.connect(url).get()
            val professorElements: Elements = doc.select("div.card.shadow-sm.my-sm-2")

            val jobs = professorElements.map { element ->
                async {
                    val nameElement = element.selectFirst("h5 a")
                    val name = nameElement?.text()?.trim() ?: "Unknown"
                    val profileUrl = nameElement?.attr("href")?.let { "https://pro.guap.ru$it" } ?: "Unknown"

                    val imageElement = element.selectFirst("img.profile_image")
                    val imageUrl = imageElement?.attr("src")?.let { "https://pro.guap.ru$it" } ?: "Unknown"

                    val positions = if (profileUrl != "Unknown") parseProfessorPage(profileUrl) else emptyList()

                    Professor(name, profileUrl, imageUrl, positions)
                }
            }

            val results = jobs.awaitAll()
            if (results.isEmpty()) {
                isMorePages = false
            } else {
                synchronized(allProfessors) {
                    allProfessors.addAll(results)
                }
                page++
            }
        } catch (e: IOException) {
            println("Error parsing page $url: ${e.message}")
        }
    }

    allProfessors
}
// Парсинг каждого преподавателя
suspend fun parseProfessorPage(url: String): List<Position> = withContext(Dispatchers.IO) {
    try {
        val doc: Document = Jsoup.connect(url).get()
        val positionElements: Elements = doc.select("div.card.shadow-sm div.card-body div.list-group-item")

        val positions = mutableSetOf<Position>()

        for (element in positionElements) {
            val department = element.selectFirst("div.small.text-end.text-muted.mb-1")?.text()?.trim()
            val title = element.selectFirst("h5.fw-semibold.my-1")?.text()?.trim()
            val institute = element.selectFirst("div.small:not(.text-end)")?.text()?.trim()

            department?.let { dep ->
                title?.let { t ->
                    institute?.let { inst ->
                        val newPosition = Position(dep, t, inst)
                        positions.add(newPosition)
                    }
                }
            }
        }

        positions.toList()
    } catch (e: IOException) {
        println("Ошибка при парсинге страницы преподавателя $url: ${e.message}")
        emptyList()
    }
}
// Получение списка фильтров (выборка)
fun extractFilters(url: String): Filters {
    val doc: Document = Jsoup.connect(url).get()

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

// Извлечение фильтров
/*
val filters = extractFilters("https://pro.guap.ru/professors")
println("Positions: ${filters.positions}")
println("Faculties: ${filters.faculties}")
println("Subunits: ${filters.subunits}")
*/