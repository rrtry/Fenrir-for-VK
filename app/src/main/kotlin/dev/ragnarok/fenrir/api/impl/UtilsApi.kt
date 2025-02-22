package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IUtilsApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.VKApiShortLink
import dev.ragnarok.fenrir.api.model.response.ResolveDomailResponse
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import dev.ragnarok.fenrir.api.services.IUtilsService
import io.reactivex.rxjava3.core.Single

internal class UtilsApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IUtilsApi {
    override fun resolveScreenName(screenName: String?): Single<ResolveDomailResponse> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.resolveScreenName(screenName)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getShortLink(url: String?, t_private: Int?): Single<VKApiShortLink> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getShortLink(url, t_private)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLastShortenedLinks(count: Int?, offset: Int?): Single<Items<VKApiShortLink>> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getLastShortenedLinks(count, offset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteFromLastShortened(key: String?): Single<Int> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.deleteFromLastShortened(key)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun checkLink(url: String?): Single<VKApiCheckedLink> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.checkLink(url)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun joinChatByInviteLink(link: String?): Single<VKApiChatResponse> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.joinChatByInviteLink(link)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getInviteLink(peer_id: Int?, reset: Int?): Single<VKApiLinkResponse> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getInviteLink(peer_id, reset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun customScript(code: String?): Single<Int> {
        return provideService(IUtilsService::class.java, TokenType.USER)
            .flatMap { service ->
                service.customScript(code)
                    .map(extractResponseWithErrorHandling())
            }
    }
}