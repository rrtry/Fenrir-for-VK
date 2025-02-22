package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IUsersApi
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.StoryResponse
import dev.ragnarok.fenrir.api.model.response.UserWallInfoResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import dev.ragnarok.fenrir.api.services.IUsersService
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.Single

internal class UsersApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IUsersApi {
    override fun getUserWallInfo(
        userId: Int,
        fields: String?,
        nameCase: String?
    ): Single<VKApiUser> {
        return provideService(IUsersService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getUserWallInfo(
                        "var user_id = Args.user_id;\n" +
                                "var fields =Args.fields;\n" +
                                "var name_case = Args.name_case;\n" +
                                "\n" +
                                "var user_info = API.users.get({\"v\":\"" + Constants.API_VERSION + "\",\"user_ids\":user_id,\n" +
                                "    \"fields\":fields, \"name_case\":name_case});\n" +
                                "\n" +
                                "var all_wall_count =API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"all\"}).count;\n" +
                                "    \n" +
                                "var owner_wall_count =API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"owner\"}).count;\n" +
                                "\n" +
                                "var postponed_wall_count =API.wall.get({\"v\":\"" + Constants.API_VERSION + "\",\"owner_id\":user_id,\n" +
                                "    \"count\":1, \"filter\":\"postponed\"}).count;\n" +
                                "\n" +
                                "return {\"user_info\": user_info, \n" +
                                "    \"all_wall_count\":all_wall_count,\n" +
                                "    \"owner_wall_count\":owner_wall_count,\n" +
                                "    \"postponed_wall_count\":postponed_wall_count\n" +
                                "};", userId, fields, nameCase
                    )
                    .map(extractResponseWithErrorHandling())
                    .map { response ->
                        if (safeCountOf(response.users) != 1) {
                            throw NotFoundException()
                        }
                        createFrom(response)
                    }
            }
    }

    override fun getFollowers(
        userId: Int?,
        offset: Int?,
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Single<Items<VKApiUser>> {
        return provideService(IUsersService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getFollowers(userId, offset, count, fields, nameCase)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getRequests(
        offset: Int?,
        count: Int?,
        extended: Int?,
        out: Int?,
        fields: String?
    ): Single<Items<VKApiUser>> {
        return provideService(IUsersService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getRequests(offset, count, extended, out, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?,
        sort: Int?,
        offset: Int?,
        count: Int?,
        fields: String?,
        city: Int?,
        country: Int?,
        hometown: String?,
        universityCountry: Int?,
        university: Int?,
        universityYear: Int?,
        universityFaculty: Int?,
        universityChair: Int?,
        sex: Int?,
        status: Int?,
        ageFrom: Int?,
        ageTo: Int?,
        birthDay: Int?,
        birthMonth: Int?,
        birthYear: Int?,
        online: Boolean?,
        hasPhoto: Boolean?,
        schoolCountry: Int?,
        schoolCity: Int?,
        schoolClass: Int?,
        school: Int?,
        schoolYear: Int?,
        religion: String?,
        interests: String?,
        company: String?,
        position: String?,
        groupId: Int?,
        fromList: String?
    ): Single<Items<VKApiUser>> {
        return provideService(IUsersService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .search(
                        query,
                        sort,
                        offset,
                        count,
                        fields,
                        city,
                        country,
                        hometown,
                        universityCountry,
                        university,
                        universityYear,
                        universityFaculty,
                        universityChair,
                        sex,
                        status,
                        ageFrom,
                        ageTo,
                        birthDay,
                        birthMonth,
                        birthYear,
                        integerFromBoolean(online),
                        integerFromBoolean(hasPhoto),
                        schoolCountry,
                        schoolCity,
                        schoolClass,
                        school,
                        schoolYear,
                        religion,
                        interests,
                        company,
                        position,
                        groupId,
                        fromList
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun report(userId: Int?, type: String?, comment: String?): Single<Int> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service
                    .report(userId, type, comment)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun checkAndAddFriend(userId: Int?): Single<Int> {
        return provideService(IUsersService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .checkAndAddFriend(
                        "var user_id = Args.user_id; if(API.users.get({\"v\":\"" + Constants.API_VERSION + "\", \"user_ids\": user_id, \"fields\": \"friend_status\"})[0].friend_status == 0) {return API.friends.add({\"v\":\"" + Constants.API_VERSION + "\", \"user_id\": user_id});} return 0;",
                        userId
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getStory(owner_id: Int?, extended: Int?, fields: String?): Single<StoryResponse> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getStory(owner_id, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getNarratives(
        owner_id: Int,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiNarratives>> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getNarratives(owner_id, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getStoryById(
        stories: List<AccessIdPair>,
        extended: Int?,
        fields: String?
    ): Single<StoryGetResponse> {
        val storyString = join(stories, ",") { AccessIdPair.format(it) }
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getStoryById(storyString, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getGifts(user_id: Int?, count: Int?, offset: Int?): Single<Items<VKApiGift>> {
        return provideService(IUsersService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getGifts(user_id, count, offset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchStory(
        q: String?,
        mentioned_id: Int?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<StoryResponse> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.searchStory(q, mentioned_id, count, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        userIds: Collection<Int>?,
        domains: Collection<String>?,
        fields: String?,
        nameCase: String?
    ): Single<List<VKApiUser>> {
        val ids = ArrayList<String?>(1)
        if (userIds != null) {
            ids.add(join(userIds, ","))
        }
        if (domains != null) {
            ids.add(join(domains, ","))
        }
        return provideService(
            IUsersService::class.java,
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMap { service ->
                service[join(ids, ","), fields, nameCase]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_getPhotoUploadServer(): Single<VKApiStoryUploadServer> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_getPhotoUploadServer(1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_getVideoUploadServer(): Single<VKApiStoryUploadServer> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_getVideoUploadServer(1)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_save(upload_results: String?): Single<Items<VKApiStory>> {
        return provideService(IUsersService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_save(upload_results)
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        private fun createFrom(response: UserWallInfoResponse): VKApiUser {
            val user = response.users?.get(0) ?: throw NotFoundException()
            if (user.counters == null) {
                user.counters = VKApiUser.Counters()
            }
            response.allWallCount.requireNonNull {
                user.counters?.all_wall = it
            }
            response.ownerWallCount.requireNonNull {
                user.counters?.owner_wall = it
            }
            response.postponedWallCount.requireNonNull {
                user.counters?.postponed_wall = it
            }
            return user
        }
    }
}