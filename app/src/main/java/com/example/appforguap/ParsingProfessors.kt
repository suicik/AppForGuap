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
    val positions: List<Position> = emptyList(),
    val subjects: List<String> = emptyList()
)
{
    constructor() : this("", "", "", emptyList(), emptyList())

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

                    val (positions, subjects) = if (profileUrl != "Unknown") parseProfessorPage(profileUrl) else Pair(emptyList(), emptyList())

                    Professor(name, profileUrl, imageUrl, positions, subjects)
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
suspend fun parseProfessorPage(url: String): Pair<List<Position>, List<String>> = withContext(Dispatchers.IO) {
    try {
        val doc: Document = Jsoup.connect(url).get()

        // Selecting position elements
        val positionElements: Elements = doc.select("div.card.shadow-sm div.card-body div.list-group-item")
        val positions = mutableListOf<Position>()

        // Parsing position elements
        for (element in positionElements) {
            val department = element.selectFirst("div.small.text-end.text-muted.mb-1")?.text()?.trim() ?: ""
            val title = element.selectFirst("h5.fw-semibold.my-1")?.text()?.trim() ?: ""
            val institute = element.selectFirst("div.small:not(.text-end)")?.text()?.trim() ?: ""

            if (department.isNotEmpty() && title.isNotEmpty() && institute.isNotEmpty()) {
                val newPosition = Position(department, title, institute)
                positions.add(newPosition)
            }
        }

        // Selecting subject elements
        val subjectElements: Elements = doc.select("div#subjects div.list-group-item")
        val subjects = subjectElements.map { it.text().trim() }

        Pair(positions, subjects)
    } catch (e: IOException) {
        println("Ошибка при парсинге страницы преподавателя $url: ${e.message}")
        Pair(emptyList(), emptyList())
    }
}
