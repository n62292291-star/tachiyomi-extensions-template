package eu.kanade.tachiyomi.extension.zh.copy2026

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Copy2026 : ParsedHttpSource() {
    override val name = "拷贝漫画 (2026)"
    override val baseUrl = "https://www.2026copy.com"
    override val lang = "zh"
    override val supportsLatest = true

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .set("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/?page=$page", headers)
    override fun popularMangaSelector() = "div.comic-item"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h3 a")?.text()?.trim() ?: ""
        thumbnail_url = element.selectFirst("img")?.attr("abs:src") ?: ""
        url = element.selectFirst("a")?.attr("href") ?: ""
    }

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/search?keyword=$query&page=$page", headers)

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1")?.text()?.trim() ?: ""
        author = document.selectFirst("span:contains(作者), .author")?.text()?.trim()
        description = document.selectFirst(".intro, .description, p")?.text()?.trim()
        thumbnail_url = document.selectFirst("img.cover, .comic-img img")?.attr("abs:src") ?: ""
    }

    override fun chapterListSelector() = "div.chapter-item a, a[href*='/chapter/']"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.text().trim()
        url = element.attr("href")
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("img[data-original], img[data-src], img[src*='photo']")
            .mapIndexed { index, el ->
                val url = el.attr("abs:data-original")
                    .ifBlank { el.attr("abs:data-src") }
                    .ifBlank { el.attr("abs:src") }
                Page(index, imageUrl = url)
            }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException()
}
