package com.rizmi.cleanshield.data.repository

import com.rizmi.cleanshield.data.local.dao.AllowlistDao
import com.rizmi.cleanshield.data.local.dao.BlocklistDao
import com.rizmi.cleanshield.data.local.entity.AllowedDomainEntity
import com.rizmi.cleanshield.data.local.entity.BlockedDomainEntity
import com.rizmi.cleanshield.data.local.entity.BlocklistSourceEntity
import com.rizmi.cleanshield.data.parser.BlocklistParser
import com.rizmi.cleanshield.domain.matcher.DomainNormalizer
import com.rizmi.cleanshield.domain.model.AllowlistRule
import com.rizmi.cleanshield.domain.model.BlocklistPreview
import com.rizmi.cleanshield.domain.model.BlocklistSource
import com.rizmi.cleanshield.domain.repository.BlocklistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.GZIPInputStream

class BlocklistRepositoryImpl(
    private val blocklistDao: BlocklistDao,
    private val allowlistDao: AllowlistDao
) : BlocklistRepository {

    override fun getAllSources(): Flow<List<BlocklistSource>> {
        return blocklistDao.getAllSources().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getLatestSource(): Flow<BlocklistSource?> {
        return blocklistDao.getLatestSource().map { it?.toDomain() }
    }

    override fun getTotalBlockedCount(): Flow<Int> {
        return blocklistDao.getTotalBlockedCount()
    }

    override suspend fun getActiveBlockedDomains(): List<String> = withContext(Dispatchers.IO) {
        blocklistDao.getAllBlockedDomainsSync()
    }

    override suspend fun previewBlocklist(
        inputStream: InputStream,
        fileName: String
    ): BlocklistPreview = withContext(Dispatchers.IO) {
        BlocklistParser.parse(inputStream, fileName)
    }

    override suspend fun applyBlocklist(
        preview: BlocklistPreview,
        uriString: String?,
        fileSizeBytes: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (preview.validCount == 0 || preview.parsedDomains.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No valid domains found in file"))
            }

            // Insert source record
            val sourceId = blocklistDao.insertSource(
                BlocklistSourceEntity(
                    name = preview.fileName,
                    uriString = uriString,
                    fileSizeBytes = fileSizeBytes,
                    totalLines = preview.totalLines,
                    validCount = preview.validCount,
                    invalidCount = preview.invalidCount,
                    duplicateCount = preview.duplicateCount,
                    importTimestamp = System.currentTimeMillis(),
                    isActive = true
                )
            )

            // Clear old blocked domains and atomic insert in batches
            blocklistDao.clearAllBlockedDomains()

            val entities = preview.parsedDomains.map { domain ->
                BlockedDomainEntity(domain = domain, sourceId = sourceId)
            }

            // Batch insert to prevent SQLite variable limits
            entities.chunked(1000).forEach { chunk ->
                blocklistDao.insertDomains(chunk)
            }

            Result.success(preview.validCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearBlocklists(): Unit = withContext(Dispatchers.IO) {
        blocklistDao.clearAllBlockedDomains()
        blocklistDao.clearAllSources()
        Unit
    }

    override suspend fun loadDefaultBlocklist(): Result<Int> = withContext(Dispatchers.IO) {
        val defaultContent = """
            # CleanShield Curated Mobile Ads & Telemetry Shield
            # Google Mobile Ads & Doubleclick
            0.0.0.0 doubleclick.net
            0.0.0.0 googleads.g.doubleclick.net
            0.0.0.0 pubads.g.doubleclick.net
            0.0.0.0 securepubads.g.doubleclick.net
            0.0.0.0 pagead2.googlesyndication.com
            0.0.0.0 afs.googlesyndication.com
            0.0.0.0 tpc.googlesyndication.com
            0.0.0.0 adservice.google.com
            0.0.0.0 admob.com
            0.0.0.0 pagead.l.doubleclick.net
            0.0.0.0 dart.l.doubleclick.net
            0.0.0.0 ads.google.com
            0.0.0.0 google-analytics.com
            0.0.0.0 app-measurement.com
            0.0.0.0 firebase-telemetry.googleapis.com
            # ByteDance / Pangle / TikTok Ads (Streaming & Video Apps)
            0.0.0.0 pangle-ads.com
            0.0.0.0 pangolin-sdk-toutiao.com
            0.0.0.0 pglstatp-toutiao.com
            0.0.0.0 api-access.pangolin-sdk-toutiao.com
            0.0.0.0 pangolin.snssdk.com
            0.0.0.0 i.snssdk.com
            0.0.0.0 log.byteoversea.com
            0.0.0.0 mon.byteoversea.com
            0.0.0.0 toblog.ctobsnssdk.com
            0.0.0.0 sf16-static.i18n-pglstatp.com
            0.0.0.0 ad.toutiao.com
            # Unity Ads
            0.0.0.0 unityads.unity3d.com
            0.0.0.0 config.unityads.unity3d.com
            0.0.0.0 auction.unityads.unity3d.com
            0.0.0.0 webview.unityads.unity3d.com
            0.0.0.0 adserver.unityads.unity3d.com
            0.0.0.0 mediation.unityads.unity3d.com
            # AppLovin / MAX Mediation
            0.0.0.0 applovin.com
            0.0.0.0 a.applovin.com
            0.0.0.0 d.applovin.com
            0.0.0.0 ms.applovin.com
            0.0.0.0 res.applovin.com
            0.0.0.0 pdn.applovin.com
            0.0.0.0 array-sdk.applovin.com
            # Mintegral / Mobvista
            0.0.0.0 mintegral.com
            0.0.0.0 adx.mintegral.com
            0.0.0.0 alphab.track.mmedia.com
            0.0.0.0 ocean.mintegral.com
            0.0.0.0 pglstat.mintegral.com
            0.0.0.0 hb.mintegral.com
            # IronSource / SupersonicAds
            0.0.0.0 ironsrc.mobi
            0.0.0.0 a.ironsrc.com
            0.0.0.0 track.ironsrc.com
            0.0.0.0 supersonicads.com
            0.0.0.0 supersonic.com
            0.0.0.0 init.supersonicads.com
            # Vungle & Liftoff
            0.0.0.0 vungle.com
            0.0.0.0 ads.vungle.com
            0.0.0.0 events.api.vungle.com
            0.0.0.0 api.vungle.com
            0.0.0.0 liftoff.io
            0.0.0.0 ad.liftoff.io
            # InMobi
            0.0.0.0 inmobi.com
            0.0.0.0 config.inmobi.com
            0.0.0.0 telemetry.inmobi.com
            0.0.0.0 c.inmobi.com
            0.0.0.0 i.inmobi.com
            # Chartboost
            0.0.0.0 chartboost.com
            0.0.0.0 live.chartboost.com
            0.0.0.0 da.chartboost.com
            # AdColony, Fyber & Digital Turbine
            0.0.0.0 adcolony.com
            0.0.0.0 adc3-launch.adcolony.com
            0.0.0.0 fyber.com
            0.0.0.0 digitalturbine.com
            0.0.0.0 inner-active.mobi
            # Tapjoy
            0.0.0.0 tapjoy.com
            0.0.0.0 ads.tapjoy.com
            0.0.0.0 rpc.tapjoy.com
            # Bigo Ads / Likee Ad Network
            0.0.0.0 bigossp.com
            0.0.0.0 ads.bigossp.com
            0.0.0.0 api.bigossp.com
            0.0.0.0 sdk.bigossp.com
            # Meta / Facebook Audience Network
            0.0.0.0 an.facebook.com
            0.0.0.0 pixel.facebook.com
            # Video & Display Ad Networks
            0.0.0.0 taboola.com
            0.0.0.0 outbrain.com
            0.0.0.0 criteo.com
            0.0.0.0 smartadserver.com
            0.0.0.0 spotxchange.com
            0.0.0.0 spotx.tv
            0.0.0.0 tremorhub.com
            0.0.0.0 teads.tv
            0.0.0.0 pubmatic.com
            0.0.0.0 openx.net
            0.0.0.0 rubiconproject.com
            0.0.0.0 magnite.com
            0.0.0.0 smaato.net
            0.0.0.0 media.net
            0.0.0.0 moatads.com
            0.0.0.0 scorecardresearch.com
            0.0.0.0 quantserve.com
            0.0.0.0 indexexchange.com
            0.0.0.0 casalemedia.com
            0.0.0.0 bidswitch.net
            0.0.0.0 sovrn.com
            0.0.0.0 yieldmo.com
            0.0.0.0 triplelift.com
            0.0.0.0 sharethrough.com
            # Popups & Aggressive Video Web Ads
            0.0.0.0 popads.net
            0.0.0.0 popcash.net
            0.0.0.0 propellerads.com
            0.0.0.0 exoclick.com
            0.0.0.0 adcash.com
            0.0.0.0 hilltopads.net
            0.0.0.0 clickadu.com
            0.0.0.0 monetag.com
            0.0.0.0 trafficstars.com
            0.0.0.0 adsterra.com
            0.0.0.0 yadro.ru
            # Mobile Telemetry & Tracking
            0.0.0.0 appsflyer.com
            0.0.0.0 adjust.com
            0.0.0.0 branch.io
            0.0.0.0 kochava.com
            0.0.0.0 singular.net
            0.0.0.0 tenjin.io
            0.0.0.0 flurry.com
            0.0.0.0 crashlytics.com
            0.0.0.0 telemetry.sdk.io
            0.0.0.0 appmetrica.yandex.net
            0.0.0.0 mopub.com
        """.trimIndent()

        val preview = previewBlocklist(
            ByteArrayInputStream(defaultContent.toByteArray(Charsets.UTF_8)),
            "CleanShield Curated Standard List"
        )
        applyBlocklist(preview, null, defaultContent.length.toLong())
    }

    override suspend fun downloadAndApplyBlocklist(
        url: String,
        name: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            var currentUrl = url
            var redirects = 0
            val maxRedirects = 5

            while (redirects < maxRedirects) {
                val endpoint = URI(currentUrl).toURL()
                val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) CleanShield/1.0")
                    setRequestProperty("Accept", "text/plain, text/html, */*")
                    setRequestProperty("Accept-Encoding", "gzip, deflate")
                }
                connection = conn

                val responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        return@withContext Result.failure(IOException("Redirect location missing"))
                    }
                    currentUrl = if (location.startsWith("http")) location else URI(currentUrl).resolve(location).toString()
                    redirects++
                    continue
                }

                if (responseCode !in 200..299) {
                    return@withContext Result.failure(IOException("Server returned HTTP $responseCode"))
                }
                break
            }

            val finalConn = connection ?: return@withContext Result.failure(IOException("Connection failed"))
            val contentLength = finalConn.contentLengthLong.let { if (it > 0) it else 0L }
            val rawStream = finalConn.inputStream
            val inputStream = if ("gzip".equals(finalConn.contentEncoding, ignoreCase = true)) {
                GZIPInputStream(rawStream)
            } else {
                rawStream
            }

            val preview = BlocklistParser.parse(inputStream, name)
            if (preview.validCount == 0) {
                return@withContext Result.failure(IllegalStateException("No valid domains parsed from blocklist source"))
            }

            applyBlocklist(preview, url, contentLength)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    // Allowlist
    override fun getAllowlist(): Flow<List<AllowlistRule>> {
        return allowlistDao.getAll().map { list ->
            list.map { AllowlistRule(it.id, it.domain, it.addedTimestamp, it.note) }
        }
    }

    override fun getAllowlistCount(): Flow<Int> {
        return allowlistDao.getCount()
    }

    override suspend fun getActiveAllowedDomains(): List<String> = withContext(Dispatchers.IO) {
        allowlistDao.getAllDomainsSync()
    }

    override suspend fun addAllowedDomain(domain: String, note: String?): Boolean = withContext(Dispatchers.IO) {
        val normalized = DomainNormalizer.normalize(domain) ?: return@withContext false
        val id = allowlistDao.insert(AllowedDomainEntity(domain = normalized, note = note))
        id > 0
    }

    override suspend fun removeAllowedDomain(domain: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = DomainNormalizer.normalize(domain) ?: return@withContext false
        allowlistDao.deleteByDomain(normalized)
        true
    }

    override suspend fun clearAllowlist(): Unit = withContext(Dispatchers.IO) {
        allowlistDao.clearAll()
        Unit
    }

    private fun BlocklistSourceEntity.toDomain() = BlocklistSource(
        id = id,
        name = name,
        uriString = uriString,
        fileSizeBytes = fileSizeBytes,
        totalLines = totalLines,
        validCount = validCount,
        invalidCount = invalidCount,
        duplicateCount = duplicateCount,
        importTimestamp = importTimestamp,
        isActive = isActive
    )
}
