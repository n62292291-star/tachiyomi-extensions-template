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

    // ==================== 章节（单本图集）===================
    override fun chapterListParse(response: Response): List<SChapter> {
        var currentPath = response.request.url.encodedPath
        if (currentPath.contains("photos-aid")) {
            currentPath = currentPath.replace("photos-aid", "photos-index-aid")
        }
        
        val chapter = SChapter.create().apply {
            name = "单行本 / 全一话"
            url = currentPath
        }
        return listOf(chapter)
    }

    // ==================== 图片（Slide 阅读模式）===================
    override fun pageListRequest(chapter: SChapter): Request {
        val slideUrl = chapter.url.replace("photos-index-aid", "photos-slide-aid")
        return GET(baseUrl + slideUrl, headers)
    }

    override fun pageListParse(document: Document): List<Page> {
        val scriptContent = document.select("script:containsData(fast_img_host), script:containsData(imglist)")
            .firstOrNull()?.data() ?: return emptyList()

        val cleanScript = scriptContent.replace("\\/", "/")

        val regex = """//[^"'\s]+\.(?:jpg|png|jpeg|webp)""".toRegex(RegexOption.IGNORE_CASE)
        val matches = regex.findAll(cleanScript)

        return matches.mapIndexed { index, match ->
            var imgUrl = match.value
            if (imgUrl.startsWith("//")) imgUrl = "https:$imgUrl"
            Page(index, imageUrl = imgUrl)
        }.toList()
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()

    // 必须显式声明返回类型，避免 Kotlin 编译错误
    override fun chapterListSelector(): String = throw UnsupportedOperationException()
    override fun chapterFromElement(element: Element): SChapter = throw UnsupportedOperationException()
}