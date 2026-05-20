
package eu.kanade.tachiyomi.extension.zh.wnacg

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

class Wnacg : ParsedHttpSource() {
    override val name = "绅士漫画 (WNACG)"
    override val baseUrl = "https://wnacg.com"
    override val lang = "zh"
    override val supportsLatest = true

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .build()

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/albums.html?page=$page", headers)

    override fun popularMangaSelector() = "div.gallery"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.select("h2 a").text()
            thumbnail_url = element.select("img").attr("abs:src")
            url = element.select("a").attr("abs:href")
        }
    }

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/albums.html?search=$query&page=$page", headers)

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("h1").text()
            description = document.select("简介或标签").text()
            thumbnail_url = document.select("img.cover").attr("abs:src")
        }
    }

    override fun chapterListSelector() = "a[href*='photos-index-aid']"  // 通常单章节

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = "第1话" // 大多是单本
            url = element.attr("abs:href")
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("img[data-original], img[src*='photo']").mapIndexed { i, el ->
            Page(i, imageUrl = el.attr("abs:data-original").ifBlank { el.attr("abs:src") })
        }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException()
}
