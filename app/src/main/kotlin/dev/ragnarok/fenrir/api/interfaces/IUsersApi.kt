package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.StoryResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import io.reactivex.rxjava3.core.Single

interface IUsersApi {
    @CheckResult
    fun getUserWallInfo(userId: Int, fields: String?, nameCase: String?): Single<VKApiUser>

    @CheckResult
    fun getFollowers(
        userId: Int?, offset: Int?, count: Int?,
        fields: String?, nameCase: String?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun getRequests(
        offset: Int?,
        count: Int?,
        extended: Int?,
        out: Int?,
        fields: String?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun search(
        query: String?, sort: Int?, offset: Int?, count: Int?,
        fields: String?, city: Int?, country: Int?, hometown: String?,
        universityCountry: Int?, university: Int?, universityYear: Int?,
        universityFaculty: Int?, universityChair: Int?, sex: Int?,
        status: Int?, ageFrom: Int?, ageTo: Int?, birthDay: Int?,
        birthMonth: Int?, birthYear: Int?, online: Boolean?,
        hasPhoto: Boolean?, schoolCountry: Int?, schoolCity: Int?,
        schoolClass: Int?, school: Int?, schoolYear: Int?,
        religion: String?, interests: String?, company: String?,
        position: String?, groupId: Int?, fromList: String?
    ): Single<Items<VKApiUser>>

    @CheckResult
    operator fun get(
        userIds: Collection<Int>?, domains: Collection<String>?,
        fields: String?, nameCase: String?
    ): Single<List<VKApiUser>>

    @CheckResult
    fun stories_getPhotoUploadServer(): Single<VKApiStoryUploadServer>

    @CheckResult
    fun stories_getVideoUploadServer(): Single<VKApiStoryUploadServer>

    @CheckResult
    fun stories_save(upload_results: String?): Single<Items<VKApiStory>>

    @CheckResult
    fun getStory(owner_id: Int?, extended: Int?, fields: String?): Single<StoryResponse>

    @CheckResult
    fun getNarratives(owner_id: Int, offset: Int?, count: Int?): Single<Items<VKApiNarratives>>

    @CheckResult
    fun getStoryById(
        stories: List<AccessIdPair>,
        extended: Int?,
        fields: String?
    ): Single<StoryGetResponse>

    @CheckResult
    fun checkAndAddFriend(userId: Int?): Single<Int>

    @CheckResult
    fun getGifts(user_id: Int?, count: Int?, offset: Int?): Single<Items<VKApiGift>>

    @CheckResult
    fun searchStory(
        q: String?,
        mentioned_id: Int?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<StoryResponse>

    @CheckResult
    fun report(userId: Int?, type: String?, comment: String?): Single<Int>
}