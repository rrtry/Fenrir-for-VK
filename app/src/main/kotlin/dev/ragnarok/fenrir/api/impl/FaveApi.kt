package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.interfaces.IFaveApi
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.api.model.response.FavePostsResponse
import dev.ragnarok.fenrir.api.services.IFaveService
import dev.ragnarok.fenrir.db.column.UserColumns
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Single

internal class FaveApi(accountId: Int, provider: IServiceProvider) : AbsApi(accountId, provider),
    IFaveApi {
    override fun getPages(
        offset: Int?,
        count: Int?,
        fields: String?,
        type: String?
    ): Single<Items<FavePageResponse>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getPages(offset, count, type, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPhotos(offset: Int?, count: Int?): Single<Items<VKApiPhoto>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getPhotos(offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getVideos(offset: Int?, count: Int?): Single<List<VKApiVideo>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getVideos(offset, count, "video", 1, UserColumns.API_FIELDS)
                    .map(extractResponseWithErrorHandling())
                    .flatMap { t ->
                        val temp = listEmptyIfNull(t.items)
                        val videos: MutableList<VKApiVideo> = ArrayList()
                        for (i in temp) {
                            if (i.attachment is VKApiVideo) videos.add(i.attachment)
                        }
                        Single.just<List<VKApiVideo>>(videos)
                    }
            }
    }

    override fun getArticles(offset: Int?, count: Int?): Single<List<VKApiArticle>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getArticles(offset, count, "article", 1, UserColumns.API_FIELDS)
                    .map(extractResponseWithErrorHandling())
                    .flatMap { t ->
                        val temp = listEmptyIfNull(t.items)
                        val articles: MutableList<VKApiArticle> = ArrayList()
                        for (i in temp) {
                            if (i.attachment is VKApiArticle) articles.add(i.attachment)
                        }
                        Single.just<List<VKApiArticle>>(articles)
                    }
            }
    }

    override fun getOwnerPublishedArticles(
        owner_id: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiArticle>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getOwnerPublishedArticles(
                    owner_id,
                    offset,
                    count,
                    "date",
                    1,
                    UserColumns.API_FIELDS
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPosts(offset: Int?, count: Int?): Single<FavePostsResponse> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getPosts(offset, count, "post", 1, UserColumns.API_FIELDS)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLinks(offset: Int?, count: Int?): Single<Items<FaveLinkDto>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getLinks(offset, count, "link", 1, UserColumns.API_FIELDS)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getProducts(offset: Int?, count: Int?): Single<List<VKApiMarket>> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.getProducts(offset, count, "product", 1, UserColumns.API_FIELDS)
                    .map(extractResponseWithErrorHandling())
                    .flatMap { t ->
                        val temp = listEmptyIfNull(t.items)
                        val markets: MutableList<VKApiMarket> = ArrayList()
                        for (i in temp) {
                            if (i.attachment is VKApiMarket) markets.add(i.attachment)
                        }
                        Single.just<List<VKApiMarket>>(markets)
                    }
            }
    }

    override fun addPage(userId: Int?, groupId: Int?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.addPage(userId, groupId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun addLink(link: String?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.addLink(link)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun addVideo(owner_id: Int?, id: Int?, access_key: String?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.addVideo(owner_id, id, access_key)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun addArticle(url: String?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.addArticle(url)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun addProduct(id: Int, owner_id: Int, access_key: String?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.addProduct(id, owner_id, access_key)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun addPost(owner_id: Int?, id: Int?, access_key: String?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.addPost(owner_id, id, access_key)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun removePage(userId: Int?, groupId: Int?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.removePage(userId, groupId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun removeLink(linkId: String?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.removeLink(linkId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun removeArticle(owner_id: Int?, article_id: Int?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.removeArticle(owner_id, article_id)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun removeProduct(id: Int?, owner_id: Int?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.removeProduct(id, owner_id)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun removePost(owner_id: Int?, id: Int?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.removePost(owner_id, id)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun removeVideo(owner_id: Int?, id: Int?): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.removeVideo(owner_id, id)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun pushFirst(owner_id: Int): Single<Boolean> {
        return provideService(IFaveService::class.java)
            .flatMap { service ->
                service.pushFirst(
                    "var owner_id = Args.owner_id;\n" +
                            "if (owner_id >= 0) {\n" +
                            "   var ret = API.fave.removePage({\"v\":\"" + Constants.API_VERSION + "\", \"user_id\":owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "   ret = API.fave.addPage({\"v\":\"" + Constants.API_VERSION + "\", \"user_id\":owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "} else {\n" +
                            "   var ret = API.fave.removePage({\"v\":\"" + Constants.API_VERSION + "\", \"group_id\":-owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "   ret = API.fave.addPage({\"v\":\"" + Constants.API_VERSION + "\", \"group_id\":-owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "}\n" +
                            "return 1;", owner_id
                )
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }
}