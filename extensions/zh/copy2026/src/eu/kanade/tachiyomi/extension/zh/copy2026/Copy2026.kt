package eu.kanade.tachiyomi.extension.zh.copy2026

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Copy2026 : ParsedHttpSource() {
    override val name = "拷贝漫画 (2026)"
    override val baseUrl = "https://www.2026copy.com"
    override val lang = "zh"
    override val supportsLatest = true

    override fun headersBuilder() = super.headersBuilder()
        .set("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
        .set("Referer", "$baseUrl/")

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .build()

    // 热门 / 首页
    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/?page=$page", headers)

    override fun popularMangaSelector() = "div.comic-item"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.select("h3 a").text()
            thumbnail_url = element.select("img").attr("abs:src")
            url = element.select("a").attr("abs:href")
        }
    }

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    // 搜索
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?keyword=$query&page=$page", headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    // 详情
    override fun mangaDetailsRequest(manga: SManga): Request = GET(manga.url, headers)

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("h1").text()
            author = document.select("作者相关").text() // 需根据实际调整
            description = document.select("简介").text()
            thumbnail_url = document.select("img.cover").attr("abs:src")
            status = SManga.COMPLETED // 或 UNKNOWN
        }
    }

    override fun chapterListSelector() = "div.chapter-item a"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = element.text()
            url = element.attr("abs:href")
            chapter_number = name.filter { it.isDigit() }.toFloatOrNull() ?: 0f
        }
    }

    // 图片
    override fun pageListParse(document: Document): List<Page> {
        return document.select("img[data-original], img[src*='photo']").mapIndexed { index, element ->
            val img = element.attr("abs:data-original").ifBlank { element.attr("abs:src") }
            Page(index, imageUrl = img)
        }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException()
}
