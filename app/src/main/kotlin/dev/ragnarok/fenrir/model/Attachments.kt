package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.adapter.DocLink
import dev.ragnarok.fenrir.adapter.PostImage
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.cloneListAsArrayList
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.Utils.safeCountOfMultiple

class Attachments : Parcelable, Cloneable {
    var audios: ArrayList<Audio>? = null
        private set
    var stickers: ArrayList<Sticker>? = null
        private set
    var photos: ArrayList<Photo>? = null
        private set
    var docs: ArrayList<Document>? = null
        private set
    var videos: ArrayList<Video>? = null
        private set
    var posts: ArrayList<Post>? = null
        private set
    var links: ArrayList<Link>? = null
        private set
    var articles: ArrayList<Article>? = null
        private set
    var stories: ArrayList<Story>? = null
        private set
    var calls: ArrayList<Call>? = null
        private set
    var polls: ArrayList<Poll>? = null
        private set
    var pages: ArrayList<WikiPage>? = null
        private set
    var voiceMessages: ArrayList<VoiceMessage>? = null
        private set
    var gifts: ArrayList<GiftItem>? = null
        private set
    var audioPlaylists: ArrayList<AudioPlaylist>? = null
        private set
    var graffity: ArrayList<Graffiti>? = null
        private set
    var photoAlbums: ArrayList<PhotoAlbum>? = null
        private set
    var notSupported: ArrayList<NotSupported>? = null
        private set
    var events: ArrayList<Event>? = null
        private set
    var markets: ArrayList<Market>? = null
        private set
    var marketAlbums: ArrayList<MarketAlbum>? = null
        private set
    var wallReplies: ArrayList<WallReply>? = null
        private set
    var audioArtists: ArrayList<AudioArtist>? = null
        private set

    constructor()
    private constructor(`in`: Parcel) {
        audios = `in`.createTypedArrayList(Audio.CREATOR)
        stickers = `in`.createTypedArrayList(Sticker.CREATOR)
        photos = `in`.createTypedArrayList(Photo.CREATOR)
        docs = `in`.createTypedArrayList(Document.CREATOR)
        videos = `in`.createTypedArrayList(Video.CREATOR)
        posts = `in`.createTypedArrayList(Post.CREATOR)
        links = `in`.createTypedArrayList(Link.CREATOR)
        articles = `in`.createTypedArrayList(Article.CREATOR)
        polls = `in`.createTypedArrayList(Poll.CREATOR)
        pages = `in`.createTypedArrayList(WikiPage.CREATOR)
        voiceMessages = `in`.createTypedArrayList(VoiceMessage.CREATOR)
        gifts = `in`.createTypedArrayList(GiftItem.CREATOR)
        stories = `in`.createTypedArrayList(Story.CREATOR)
        calls = `in`.createTypedArrayList(Call.CREATOR)
        audioPlaylists = `in`.createTypedArrayList(AudioPlaylist.CREATOR)
        graffity = `in`.createTypedArrayList(Graffiti.CREATOR)
        photoAlbums = `in`.createTypedArrayList(PhotoAlbum.CREATOR)
        notSupported = `in`.createTypedArrayList(NotSupported.CREATOR)
        events = `in`.createTypedArrayList(Event.CREATOR)
        markets = `in`.createTypedArrayList(Market.CREATOR)
        marketAlbums = `in`.createTypedArrayList(MarketAlbum.CREATOR)
        wallReplies = `in`.createTypedArrayList(WallReply.CREATOR)
        audioArtists = `in`.createTypedArrayList(AudioArtist.CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(audios)
        dest.writeTypedList(stickers)
        dest.writeTypedList(photos)
        dest.writeTypedList(docs)
        dest.writeTypedList(videos)
        dest.writeTypedList(posts)
        dest.writeTypedList(links)
        dest.writeTypedList(articles)
        dest.writeTypedList(polls)
        dest.writeTypedList(pages)
        dest.writeTypedList(voiceMessages)
        dest.writeTypedList(gifts)
        dest.writeTypedList(stories)
        dest.writeTypedList(calls)
        dest.writeTypedList(audioPlaylists)
        dest.writeTypedList(graffity)
        dest.writeTypedList(photoAlbums)
        dest.writeTypedList(notSupported)
        dest.writeTypedList(events)
        dest.writeTypedList(markets)
        dest.writeTypedList(marketAlbums)
        dest.writeTypedList(wallReplies)
        dest.writeTypedList(audioArtists)
    }

    fun add(model: AbsModel) {
        if (model is Audio) {
            prepareAudios().add(model)
            return
        }
        if (model is Sticker) {
            prepareStickers().add(model)
            return
        }
        if (model is PhotoAlbum) {
            preparePhotoAlbums().add(model)
            return
        }
        if (model is Photo) {
            preparePhotos().add(model)
            return
        }
        if (model is VoiceMessage) {
            prepareVoiceMessages().add(model)
            return
        }
        if (model is Document) {
            prepareDocs().add(model)
            return
        }
        if (model is Video) {
            prepareVideos().add(model)
            return
        }
        if (model is Post) {
            preparePosts().add(model)
            return
        }
        if (model is Link) {
            prepareLinks().add(model)
            return
        }
        if (model is Article) {
            prepareArticles().add(model)
            return
        }
        if (model is Story) {
            prepareStories().add(model)
            return
        }
        if (model is Call) {
            prepareCalls().add(model)
            return
        }
        if (model is NotSupported) {
            prepareNotSupporteds().add(model)
            return
        }
        if (model is Event) {
            prepareEvents().add(model)
            return
        }
        if (model is Market) {
            prepareMarkets().add(model)
            return
        }
        if (model is MarketAlbum) {
            prepareMarketAlbums().add(model)
            return
        }
        if (model is AudioArtist) {
            prepareAudioArtist().add(model)
            return
        }
        if (model is WallReply) {
            prepareWallReply().add(model)
            return
        }
        if (model is AudioPlaylist) {
            prepareAudioPlaylists().add(model)
            return
        }
        if (model is Graffiti) {
            prepareGraffity().add(model)
            return
        }
        if (model is Poll) {
            preparePolls().add(model)
            return
        }
        if (model is WikiPage) {
            prepareWikiPages().add(model)
        }
        if (model is GiftItem) {
            prepareGifts().add(model)
        }
    }

    fun toList(): ArrayList<AbsModel> {
        val result = ArrayList<AbsModel>()
        audios.nonNullNoEmpty {
            result.addAll(it)
        }
        stickers.nonNullNoEmpty {
            result.addAll(it)
        }
        photoAlbums.nonNullNoEmpty {
            result.addAll(it)
        }
        photos.nonNullNoEmpty {
            result.addAll(it)
        }
        docs.nonNullNoEmpty {
            result.addAll(it)
        }
        voiceMessages.nonNullNoEmpty {
            result.addAll(it)
        }
        videos.nonNullNoEmpty {
            result.addAll(it)
        }
        posts.nonNullNoEmpty {
            result.addAll(it)
        }
        links.nonNullNoEmpty {
            result.addAll(it)
        }
        articles.nonNullNoEmpty {
            result.addAll(it)
        }
        stories.nonNullNoEmpty {
            result.addAll(it)
        }
        calls.nonNullNoEmpty {
            result.addAll(it)
        }
        audioPlaylists.nonNullNoEmpty {
            result.addAll(it)
        }
        notSupported.nonNullNoEmpty {
            result.addAll(it)
        }
        events.nonNullNoEmpty {
            result.addAll(it)
        }
        markets.nonNullNoEmpty {
            result.addAll(it)
        }
        marketAlbums.nonNullNoEmpty {
            result.addAll(it)
        }
        audioArtists.nonNullNoEmpty {
            result.addAll(it)
        }
        wallReplies.nonNullNoEmpty {
            result.addAll(it)
        }
        graffity.nonNullNoEmpty {
            result.addAll(it)
        }
        polls.nonNullNoEmpty {
            result.addAll(it)
        }
        pages.nonNullNoEmpty {
            result.addAll(it)
        }
        gifts.nonNullNoEmpty {
            result.addAll(it)
        }
        return result
    }

    fun prepareAudios(): ArrayList<Audio> {
        if (audios == null) {
            audios = ArrayList(1)
        }
        return audios!!
    }

    fun prepareWikiPages(): ArrayList<WikiPage> {
        if (pages == null) {
            pages = ArrayList(1)
        }
        return pages!!
    }

    fun preparePhotos(): ArrayList<Photo> {
        if (photos == null) {
            photos = ArrayList(1)
        }
        return photos!!
    }

    fun prepareVideos(): ArrayList<Video> {
        if (videos == null) {
            videos = ArrayList(1)
        }
        return videos!!
    }

    fun prepareLinks(): ArrayList<Link> {
        if (links == null) {
            links = ArrayList(1)
        }
        return links!!
    }

    fun prepareArticles(): ArrayList<Article> {
        if (articles == null) {
            articles = ArrayList(1)
        }
        return articles!!
    }

    fun prepareStories(): ArrayList<Story> {
        if (stories == null) {
            stories = ArrayList(1)
        }
        return stories!!
    }

    fun prepareCalls(): ArrayList<Call> {
        if (calls == null) {
            calls = ArrayList(1)
        }
        return calls!!
    }

    fun prepareWallReply(): ArrayList<WallReply> {
        if (wallReplies == null) {
            wallReplies = ArrayList(1)
        }
        return wallReplies!!
    }

    fun prepareNotSupporteds(): ArrayList<NotSupported> {
        if (notSupported == null) {
            notSupported = ArrayList(1)
        }
        return notSupported!!
    }

    fun prepareEvents(): ArrayList<Event> {
        if (events == null) {
            events = ArrayList(1)
        }
        return events!!
    }

    fun prepareMarkets(): ArrayList<Market> {
        if (markets == null) {
            markets = ArrayList(1)
        }
        return markets!!
    }

    fun prepareMarketAlbums(): ArrayList<MarketAlbum> {
        if (marketAlbums == null) {
            marketAlbums = ArrayList(1)
        }
        return marketAlbums!!
    }

    fun prepareAudioArtist(): ArrayList<AudioArtist> {
        if (audioArtists == null) {
            audioArtists = ArrayList(1)
        }
        return audioArtists!!
    }

    fun prepareAudioPlaylists(): ArrayList<AudioPlaylist> {
        if (audioPlaylists == null) {
            audioPlaylists = ArrayList(1)
        }
        return audioPlaylists!!
    }

    fun prepareGraffity(): ArrayList<Graffiti> {
        if (graffity == null) {
            graffity = ArrayList(1)
        }
        return graffity!!
    }

    fun prepareDocs(): ArrayList<Document> {
        if (docs == null) {
            docs = ArrayList(1)
        }
        return docs!!
    }

    fun prepareVoiceMessages(): ArrayList<VoiceMessage> {
        if (voiceMessages == null) {
            voiceMessages = ArrayList(1)
        }
        return voiceMessages!!
    }

    fun preparePolls(): ArrayList<Poll> {
        if (polls == null) {
            polls = ArrayList(1)
        }
        return polls!!
    }

    fun prepareStickers(): ArrayList<Sticker> {
        if (stickers == null) {
            stickers = ArrayList(1)
        }
        return stickers!!
    }

    fun preparePhotoAlbums(): ArrayList<PhotoAlbum> {
        if (photoAlbums == null) {
            photoAlbums = ArrayList(1)
        }
        return photoAlbums!!
    }

    fun preparePosts(): ArrayList<Post> {
        if (posts == null) {
            posts = ArrayList(1)
        }
        return posts!!
    }

    fun prepareGifts(): ArrayList<GiftItem> {
        if (gifts == null) {
            gifts = ArrayList(1)
        }
        return gifts!!
    }

    fun size(): Int {
        return safeCountOfMultiple(
            audios,
            stickers,
            photos,
            docs,
            videos,
            posts,
            links,
            articles,
            stories,
            photoAlbums,
            calls,
            audioPlaylists,
            graffity,
            polls,
            pages,
            voiceMessages,
            gifts,
            notSupported,
            events,
            markets,
            marketAlbums,
            wallReplies,
            audioArtists
        )
    }

    fun size_no_stickers(): Int {
        return safeCountOfMultiple(
            audios,
            photos,
            docs,
            videos,
            posts,
            links,
            articles,
            stories,
            photoAlbums,
            calls,
            audioPlaylists,
            graffity,
            polls,
            pages,
            voiceMessages,
            gifts,
            notSupported,
            events,
            markets,
            marketAlbums,
            wallReplies,
            audioArtists
        )
    }

    val isEmptyAttachments: Boolean
        get() = size() <= 0

    val hasAttachments: Boolean
        get() = size() > 0

    override fun describeContents(): Int {
        return 0
    }

    val postImagesVideos: ArrayList<PostImage>
        get() {
            val result = ArrayList<PostImage>(safeCountOf(videos))
            if (videos != null) {
                for (video in videos.orEmpty()) {
                    result.add(PostImage(video, PostImage.TYPE_VIDEO))
                }
            }
            return result
        }
    val postImages: ArrayList<PostImage>
        get() {
            val result = ArrayList<PostImage>(safeCountOfMultiple(photos, videos))
            if (photos != null) {
                for (photo in photos.orEmpty()) {
                    result.add(PostImage(photo, PostImage.TYPE_IMAGE))
                }
            }
            if (docs != null) {
                for (document in docs.orEmpty()) {
                    if (document.isGif && document.photoPreview != null) {
                        result.add(PostImage(document, PostImage.TYPE_GIF))
                    }
                }
            }
            return result
        }

    fun getDocLinks(postsAsLink: Boolean, excludeGifWithImages: Boolean): ArrayList<DocLink> {
        val result = ArrayList<DocLink>()
        if (docs != null) {
            for (doc in docs.orEmpty()) {
                if (excludeGifWithImages && doc.isGif && doc.photoPreview != null) {
                    continue
                }
                result.add(DocLink(doc))
            }
        }
        if (postsAsLink && posts != null) {
            for (post in posts.orEmpty()) {
                result.add(DocLink(post))
            }
        }
        if (links != null) {
            for (link in links.orEmpty()) {
                result.add(DocLink(link))
            }
        }
        if (polls != null) {
            for (poll in polls.orEmpty()) {
                result.add(DocLink(poll))
            }
        }
        if (pages != null) {
            for (page in pages.orEmpty()) {
                result.add(DocLink(page))
            }
        }
        if (stories != null) {
            for (story in stories.orEmpty()) {
                result.add(DocLink(story))
            }
        }
        if (calls != null) {
            for (call in calls.orEmpty()) {
                result.add(DocLink(call))
            }
        }
        if (audioPlaylists != null) {
            for (playlist in audioPlaylists.orEmpty()) {
                result.add(DocLink(playlist))
            }
        }
        if (graffity != null) {
            for (graff in graffity.orEmpty()) {
                result.add(DocLink(graff))
            }
        }
        if (photoAlbums != null) {
            for (album in photoAlbums.orEmpty()) {
                result.add(DocLink(album))
            }
        }
        if (notSupported != null) {
            for (ns in notSupported.orEmpty()) {
                result.add(DocLink(ns))
            }
        }
        if (events != null) {
            for (event in events.orEmpty()) {
                result.add(DocLink(event))
            }
        }
        if (markets != null) {
            for (market in markets.orEmpty()) {
                result.add(DocLink(market))
            }
        }
        if (marketAlbums != null) {
            for (market_album in marketAlbums.orEmpty()) {
                result.add(DocLink(market_album))
            }
        }
        if (audioArtists != null) {
            for (audio_artist in audioArtists.orEmpty()) {
                result.add(DocLink(audio_artist))
            }
        }
        if (wallReplies != null) {
            for (ns in wallReplies.orEmpty()) {
                result.add(DocLink(ns))
            }
        }
        return result
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Attachments {
        val clone = super.clone() as Attachments
        clone.audios = cloneListAsArrayList(audios)
        clone.stickers = cloneListAsArrayList(stickers)
        clone.photos = cloneListAsArrayList(photos)
        clone.docs = cloneListAsArrayList(docs)
        clone.videos = cloneListAsArrayList(videos)
        clone.posts = cloneListAsArrayList(posts)
        clone.links = cloneListAsArrayList(links)
        clone.articles = cloneListAsArrayList(articles)
        clone.stories = cloneListAsArrayList(stories)
        clone.photoAlbums = cloneListAsArrayList(photoAlbums)
        clone.calls = cloneListAsArrayList(calls)
        clone.audioPlaylists = cloneListAsArrayList(audioPlaylists)
        clone.graffity = cloneListAsArrayList(graffity)
        clone.polls = cloneListAsArrayList(polls)
        clone.pages = cloneListAsArrayList(pages)
        clone.voiceMessages = cloneListAsArrayList(voiceMessages)
        clone.notSupported = cloneListAsArrayList(notSupported)
        clone.events = cloneListAsArrayList(events)
        clone.markets = cloneListAsArrayList(markets)
        clone.marketAlbums = cloneListAsArrayList(marketAlbums)
        clone.audioArtists = cloneListAsArrayList(audioArtists)
        clone.wallReplies = cloneListAsArrayList(wallReplies)
        return clone
    }

    override fun toString(): String {
        var line = ""
        if (audios != null) {
            line = line + " audios=" + safeCountOf(audios)
        }
        if (stickers != null) {
            line = line + " stickers=" + safeCountOf(stickers)
        }
        if (photos != null) {
            line = line + " photos=" + safeCountOf(photos)
        }
        if (docs != null) {
            line = line + " docs=" + safeCountOf(docs)
        }
        if (videos != null) {
            line = line + " videos=" + safeCountOf(videos)
        }
        if (posts != null) {
            line = line + " posts=" + safeCountOf(posts)
        }
        if (links != null) {
            line = line + " links=" + safeCountOf(links)
        }
        if (articles != null) {
            line = line + " articles=" + safeCountOf(articles)
        }
        if (stories != null) {
            line = line + " stories=" + safeCountOf(stories)
        }
        if (photoAlbums != null) {
            line = line + " photo_albums=" + safeCountOf(photoAlbums)
        }
        if (calls != null) {
            line = line + " calls=" + safeCountOf(calls)
        }
        if (audioPlaylists != null) {
            line = line + " audio_playlists=" + safeCountOf(audioPlaylists)
        }
        if (graffity != null) {
            line = line + " graffity=" + safeCountOf(graffity)
        }
        if (polls != null) {
            line = line + " polls=" + safeCountOf(polls)
        }
        if (pages != null) {
            line = line + " pages=" + safeCountOf(pages)
        }
        if (voiceMessages != null) {
            line = line + " voiceMessages=" + safeCountOf(voiceMessages)
        }
        if (gifts != null) {
            line = line + " gifts=" + safeCountOf(gifts)
        }
        if (notSupported != null) {
            line = line + " not_supported=" + safeCountOf(notSupported)
        }
        if (events != null) {
            line = line + " events=" + safeCountOf(events)
        }
        if (markets != null) {
            line = line + " markets=" + safeCountOf(markets)
        }
        if (marketAlbums != null) {
            line = line + " market_albums=" + safeCountOf(marketAlbums)
        }
        if (wallReplies != null) {
            line = line + " wall_replies=" + safeCountOf(wallReplies)
        }
        if (audioArtists != null) {
            line = line + " audioArtists=" + safeCountOf(audioArtists)
        }
        return line.trim { it <= ' ' }
    }

    fun setPosts(posts: ArrayList<Post>?) {
        this.posts = posts
    }

    companion object CREATOR : Parcelable.Creator<Attachments> {
        override fun createFromParcel(parcel: Parcel): Attachments {
            return Attachments(parcel)
        }

        override fun newArray(size: Int): Array<Attachments?> {
            return arrayOfNulls(size)
        }
    }
}