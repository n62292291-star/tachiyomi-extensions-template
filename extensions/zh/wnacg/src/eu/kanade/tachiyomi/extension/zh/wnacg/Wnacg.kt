package eu.kanade.tachiyomi.extension.zh.wnacg

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

class Wnacg : ParsedHttpSource() {
    override val name = "绅士漫画 (WNACG)"
    override val baseUrl = "https://wnacg.com"
    override val lang = "zh"
    override val supportsLatest = true

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .set("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")

    // ==================== 列表 ====================
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/albums.html?page=$page", headers)
    override fun popularMangaSelector() = "div.gallery"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h2 a")?.text()?.trim() ?: ""
        thumbnail_url = element.selectFirst("img")?.attr("abs:src") ?: ""
        url = element.selectFirst("a")?.attr("href") ?: ""
    }

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    // ==================== 搜索 ====================
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/albums.html?search=$query&page=$page", headers)

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    // ==================== 详情 ====================
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1")?.text()?.trim() ?: ""
        description = document.selectFirst("p, .info, .description")?.text()?.trim()
        thumbnail_url = document.selectFirst("img.cover, .pic img")?.attr("abs:src") ?: ""
    }

    // ==================== 章节 ====================
    override fun chapterListSelector() = "a[href*='photos-index-aid']"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.text().ifBlank { "正文" }
        url = element.attr("href")
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    // ==================== 图片 ====================
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