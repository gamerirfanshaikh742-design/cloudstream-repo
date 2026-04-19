package com.onlykdrama

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import android.util.Base64
import org.jsoup.nodes.Element

class OnlyKdrama : MainAPI() {

    override var name = "OnlyKdrama"
    override var mainUrl = "https://onlykdrama.top"
    override var lang = "en"

    override val supportedTypes = setOf(
        TvType.TvSeries,
        TvType.AsianDrama
    )

    override suspend fun search(query: String): List<SearchResponse> {

        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document

        return document.select("article")
            .mapNotNull {
                it.toSearchResult()
            }
    }

    override suspend fun load(url: String): LoadResponse {

        val document = app.get(url).document

        val title =
            document.selectFirst("h1")?.text()
                ?: "No Title"

        val poster =
            document.selectFirst("img")?.attr("src")

        val episodes = mutableListOf<Episode>()

        document.select("a[href*=episode]")
            .forEach {

                val link = it.attr("href")

                episodes.add(
                    Episode(
                        link,
                        it.text()
                    )
                )
            }

        return newTvSeriesLoadResponse(
            title,
            url,
            TvType.TvSeries,
            episodes
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document

        val links = document.select("a[href*=continue.php]")

        for (link in links) {

            val href = link.attr("href")

            if (href.contains("d=")) {

                val encoded =
                    href.substringAfter("d=")

                try {

                    val decoded =
                        String(
                            Base64.decode(
                                encoded,
                                Base64.DEFAULT
                            )
                        )

                    val realUrl =
                        Regex("\"url\":\"(.*?)\"")
                            .find(decoded)
                            ?.groupValues?.get(1)

                    if (realUrl != null) {

                        loadExtractor(
                            realUrl,
                            data,
                            subtitleCallback,
                            callback
                        )
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return true
    }
}

fun Element.toSearchResult(): SearchResponse? {

    val title =
        this.selectFirst("h2")?.text()
            ?: return null

    val href =
        this.selectFirst("a")?.attr("href")
            ?: return null

    val poster =
        this.selectFirst("img")?.attr("src")

    return newTvSeriesSearchResponse(
        title,
        href,
        TvType.TvSeries
    ) {
        this.posterUrl = poster
    }
}
