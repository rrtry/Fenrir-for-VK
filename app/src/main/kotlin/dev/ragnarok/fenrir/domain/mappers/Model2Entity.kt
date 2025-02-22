package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.db.model.entity.*
import dev.ragnarok.fenrir.db.model.entity.AudioArtistDboEntity.AudioArtistImageEntity
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.GraffitiDbo
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity.VideoPreviewDbo
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity.ButtonEntity
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity.SourceDbo
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity.AnimationEntity
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAndAdd
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.AudioArtist.AudioArtistImage
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull

object Model2Entity {

    fun buildKeyboardEntity(keyboard: Keyboard?): KeyboardEntity? {
        val buttonsDto = keyboard?.buttons
        if (buttonsDto.isNullOrEmpty()) {
            return null
        }
        val buttons: MutableList<List<ButtonEntity>> = ArrayList(buttonsDto.size)
        for (i in buttonsDto) {
            val vt: MutableList<ButtonEntity> = ArrayList(i.size)
            for (s in i) {
                vt.add(
                    ButtonEntity().setType(s.type).setColor(s.color).setLabel(s.label)
                        .setLink(s.link).setPayload(s.payload)
                )
            }
            buttons.add(vt)
        }
        return KeyboardEntity().setAuthor_id(
            keyboard.author_id
        ).setInline(keyboard.inline)
            .setOne_time(keyboard.one_time).setButtons(buttons)
    }

    fun buildSimpleDialog(entity: Conversation): SimpleDialogEntity {
        return SimpleDialogEntity(entity.getId())
            .setInRead(entity.getInRead())
            .setOutRead(entity.getOutRead())
            .setPhoto50(entity.getPhoto50())
            .setPhoto100(entity.getPhoto100())
            .setPhoto200(entity.getPhoto200())
            .setUnreadCount(entity.getUnreadCount())
            .setTitle(entity.getTitle())
            .setPinned(entity.getPinned()?.let { buildMessageEntity(it) })
            .setAcl(entity.getAcl())
            .setGroupChannel(entity.isGroupChannel())
            .setCurrentKeyboard(buildKeyboardEntity(entity.getCurrentKeyboard()))
            .setMajor_id(entity.getMajor_id())
            .setMinor_id(entity.getMinor_id())
    }


    fun buildDialog(model: Dialog): DialogDboEntity {
        return DialogDboEntity(model.peerId)
            .setUnreadCount(model.unreadCount)
            .setInRead(model.inRead)
            .setOutRead(model.outRead)
            .setMessage(model.message?.let { buildMessageEntity(it) })
            .setLastMessageId(model.lastMessageId)
            .setTitle(model.getTitle())
            .setGroupChannel(model.isGroupChannel)
            .setPhoto50(model.photo50)
            .setPhoto100(model.photo100)
            .setPhoto200(model.photo200)
            .setMajor_id(model.major_id)
            .setMinor_id(model.getMinor_id())
    }


    fun buildMessageEntity(message: Message): MessageDboEntity {
        return MessageDboEntity().set(message.getObjectId(), message.peerId, message.senderId)
            .setDate(message.date)
            .setOut(message.isOut)
            .setBody(message.body)
            .setEncrypted(message.cryptStatus != CryptStatus.NO_ENCRYPTION)
            .setImportant(message.isImportant)
            .setDeleted(message.isDeleted)
            .setDeletedForAll(message.isDeletedForAll)
            .setForwardCount(message.forwardMessagesCount)
            .setHasAttachmens(message.isHasAttachments)
            .setStatus(message.status)
            .setOriginalId(message.originalId)
            .setAction(message.action)
            .setActionMemberId(message.actionMid)
            .setActionEmail(message.actionEmail)
            .setActionText(message.actionText)
            .setPhoto50(message.photo50)
            .setPhoto100(message.photo100)
            .setPhoto200(message.photo200)
            .setRandomId(message.randomId)
            .setExtras(message.extras)
            .setAttachments(message.attachments?.let { buildEntityAttachments(it) })
            .setForwardMessages(
                mapAll(
                    message.fwd
                ) { buildMessageEntity(it) }
            )
            .setUpdateTime(message.updateTime)
            .setPayload(message.payload)
            .setKeyboard(buildKeyboardEntity(message.keyboard))
    }

    private fun buildEntityAttachments(attachments: Attachments): List<DboEntity> {
        val entities: MutableList<DboEntity> = ArrayList(attachments.size())
        mapAndAdd(
            attachments.audios,
            { buildAudioEntity(it) },
            entities
        )
        mapAndAdd(
            attachments.stickers,
            { buildStickerEntity(it) },
            entities
        )
        mapAndAdd(
            attachments.photos,
            { buildPhotoEntity(it) },
            entities
        )
        mapAndAdd(
            attachments.docs,
            { buildDocumentDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.voiceMessages,
            { mapAudio(it) },
            entities
        )
        mapAndAdd(
            attachments.videos,
            { buildVideoDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.posts,
            { buildPostDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.links,
            { buildLinkDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.articles,
            { buildArticleDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.stories,
            { buildStoryDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.calls,
            { buildCallDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.wallReplies,
            { buildWallReplyDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.notSupported,
            { buildNotSupportedDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.events,
            { buildEventDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.markets,
            { buildMarketDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.marketAlbums,
            { buildMarketAlbumDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.audioArtists,
            { buildAudioArtistDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.graffity,
            { buildGraffityDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.audioPlaylists,
            { buildAudioPlaylistEntity(it) },
            entities
        )
        mapAndAdd(
            attachments.polls,
            { buildPollDbo(it) },
            entities
        )
        mapAndAdd(
            attachments.pages,
            { buildPageEntity(it) },
            entities
        )
        mapAndAdd(
            attachments.photoAlbums,
            { buildPhotoAlbumEntity(it) },
            entities
        )
        mapAndAdd(
            attachments.gifts,
            { buildGiftItemEntity(it) },
            entities
        )
        return entities
    }


    fun buildDboAttachments(models: List<AbsModel>): List<DboEntity> {
        val entities: MutableList<DboEntity> = ArrayList(models.size)
        for (model in models) {
            when (model) {
                is Audio -> {
                    entities.add(buildAudioEntity(model))
                }
                is Sticker -> {
                    entities.add(buildStickerEntity(model))
                }
                is Photo -> {
                    entities.add(buildPhotoEntity(model))
                }
                is Document -> {
                    entities.add(buildDocumentDbo(model))
                }
                is Video -> {
                    entities.add(buildVideoDbo(model))
                }
                is Post -> {
                    entities.add(buildPostDbo(model))
                }
                is Link -> {
                    entities.add(buildLinkDbo(model))
                }
                is Article -> {
                    entities.add(buildArticleDbo(model))
                }
                is PhotoAlbum -> {
                    entities.add(buildPhotoAlbumEntity(model))
                }
                is Story -> {
                    entities.add(buildStoryDbo(model))
                }
                is AudioPlaylist -> {
                    entities.add(buildAudioPlaylistEntity(model))
                }
                is Call -> {
                    entities.add(buildCallDbo(model))
                }
                is NotSupported -> {
                    entities.add(buildNotSupportedDbo(model))
                }
                is Event -> {
                    entities.add(buildEventDbo(model))
                }
                is Market -> {
                    entities.add(buildMarketDbo(model))
                }
                is MarketAlbum -> {
                    entities.add(buildMarketAlbumDbo(model))
                }
                is AudioArtist -> {
                    entities.add(buildAudioArtistDbo(model))
                }
                is WallReply -> {
                    entities.add(buildWallReplyDbo(model))
                }
                is Graffiti -> {
                    entities.add(buildGraffityDbo(model))
                }
                is Poll -> {
                    entities.add(buildPollDbo(model))
                }
                is WikiPage -> {
                    entities.add(buildPageEntity(model))
                }
                is GiftItem -> {
                    entities.add(buildGiftItemEntity(model))
                }
                else -> {
                    throw UnsupportedOperationException("Unsupported model")
                }
            }
        }
        return entities
    }

    private fun buildGiftItemEntity(giftItem: GiftItem): GiftItemDboEntity {
        return GiftItemDboEntity().setId(giftItem.id)
            .setThumb256(giftItem.thumb256)
            .setThumb96(giftItem.thumb96)
            .setThumb48(giftItem.thumb48)
    }

    private fun buildPageEntity(page: WikiPage): PageDboEntity {
        return PageDboEntity().set(page.id, page.ownerId)
            .setViewUrl(page.viewUrl)
            .setViews(page.views)
            .setParent2(page.parent2)
            .setParent(page.parent)
            .setCreationTime(page.creationTime)
            .setEditionTime(page.editionTime)
            .setCreatorId(page.creatorId)
            .setSource(page.source)
    }

    private fun mapAnswer(answer: Poll.Answer): PollDboEntity.Answer {
        return PollDboEntity.Answer().set(answer.id, answer.text, answer.voteCount, answer.rate)
    }

    private fun buildPollDbo(poll: Poll): PollDboEntity {
        return PollDboEntity().set(poll.id, poll.ownerId)
            .setAnswers(
                mapAll(
                    poll.answers
                ) { mapAnswer(it) }
            )
            .setQuestion(poll.question)
            .setVoteCount(poll.voteCount)
            .setMyAnswerIds(poll.myAnswerIds)
            .setCreationTime(poll.creationTime)
            .setAnonymous(poll.isAnonymous)
            .setBoard(poll.isBoard)
            .setClosed(poll.isClosed)
            .setAuthorId(poll.authorId)
            .setCanVote(poll.isCanVote)
            .setCanEdit(poll.isCanEdit)
            .setCanReport(poll.isCanReport)
            .setCanShare(poll.isCanShare)
            .setEndDate(poll.endDate)
            .setMultiple(poll.isMultiple)
            .setPhoto(poll.photo)
    }

    private fun buildLinkDbo(link: Link): LinkDboEntity {
        return LinkDboEntity().setUrl(link.url)
            .setPhoto(link.photo?.let { buildPhotoEntity(it) })
            .setTitle(link.title)
            .setDescription(link.description)
            .setCaption(link.caption)
            .setPreviewPhoto(link.previewPhoto)
    }

    private fun buildArticleDbo(dbo: Article): ArticleDboEntity {
        return ArticleDboEntity().set(dbo.id, dbo.ownerId)
            .setAccessKey(dbo.accessKey)
            .setOwnerName(dbo.ownerName)
            .setPhoto(dbo.photo?.let { buildPhotoEntity(it) })
            .setTitle(dbo.title)
            .setSubTitle(dbo.subTitle)
            .setURL(dbo.uRL)
            .setIsFavorite(dbo.isFavorite)
    }

    private fun buildStoryDbo(dbo: Story): StoryDboEntity {
        return StoryDboEntity().setId(dbo.id)
            .setOwnerId(dbo.ownerId)
            .setDate(dbo.date)
            .setExpires(dbo.expires)
            .setIs_expired(dbo.isIs_expired)
            .setAccessKey(dbo.accessKey)
            .setTarget_url(dbo.target_url)
            .setPhoto(dbo.photo?.let { buildPhotoEntity(it) })
            .setVideo(dbo.video?.let { buildVideoDbo(it) })
    }

    private fun buildCallDbo(dbo: Call): CallDboEntity {
        return CallDboEntity().setInitiator_id(dbo.initiator_id)
            .setReceiver_id(dbo.receiver_id)
            .setState(dbo.state)
            .setTime(dbo.time)
    }

    private fun buildWallReplyDbo(dbo: WallReply): WallReplyDboEntity {
        val comment = WallReplyDboEntity().setId(dbo.getObjectId())
            .setOwnerId(dbo.ownerId)
            .setFromId(dbo.fromId)
            .setPostId(dbo.postId)
            .setText(dbo.text)

        comment.setAttachments(dbo.attachments?.let { buildEntityAttachments(it) })
        return comment
    }

    private fun buildNotSupportedDbo(dbo: NotSupported): NotSupportedDboEntity {
        return NotSupportedDboEntity().setType(dbo.getType()).setBody(dbo.getBody())
    }

    private fun buildEventDbo(dbo: Event): EventDboEntity {
        return EventDboEntity().setId(dbo.id).setButton_text(dbo.button_text).setText(dbo.text)
    }

    private fun buildMarketDbo(dbo: Market): MarketDboEntity {
        return MarketDboEntity().set(dbo.id, dbo.owner_id)
            .setAccess_key(dbo.access_key)
            .setIs_favorite(dbo.isIs_favorite)
            .setAvailability(dbo.availability)
            .setDate(dbo.date)
            .setDescription(dbo.description)
            .setDimensions(dbo.dimensions)
            .setPrice(dbo.price)
            .setSku(dbo.sku)
            .setTitle(dbo.title)
            .setWeight(dbo.weight)
            .setPhotos(mapAll(dbo.photos) { buildPhotoEntity(it) })
            .setThumb_photo(dbo.thumb_photo)
    }

    private fun buildMarketAlbumDbo(dbo: MarketAlbum): MarketAlbumDboEntity {
        return MarketAlbumDboEntity().set(dbo.getId(), dbo.getOwner_id())
            .setAccess_key(dbo.getAccess_key())
            .setCount(dbo.getCount())
            .setTitle(dbo.getTitle())
            .setUpdated_time(dbo.getUpdated_time())
            .setPhoto(dbo.getPhoto()?.let { buildPhotoEntity(it) })
    }

    private fun mapArtistImage(dbo: AudioArtistImage): AudioArtistImageEntity {
        return AudioArtistImageEntity().set(dbo.url, dbo.width, dbo.height)
    }

    private fun buildAudioArtistDbo(dbo: AudioArtist): AudioArtistDboEntity {
        return AudioArtistDboEntity()
            .setId(dbo.getId())
            .setName(dbo.getName())
            .setPhoto(mapAll(dbo.getPhoto()) {
                mapArtistImage(
                    it
                )
            })
    }

    private fun buildGraffityDbo(dbo: Graffiti): GraffitiDboEntity {
        return GraffitiDboEntity().setId(dbo.id)
            .setOwner_id(dbo.owner_id)
            .setAccess_key(dbo.access_key)
            .setHeight(dbo.height)
            .setWidth(dbo.width)
            .setUrl(dbo.url)
    }


    fun buildPostDbo(post: Post): PostDboEntity {
        val dbo = PostDboEntity().set(post.vkid, post.ownerId)
            .setFromId(post.authorId)
            .setDate(post.date)
            .setText(post.text)
            .setReplyOwnerId(post.replyOwnerId)
            .setReplyPostId(post.replyPostId)
            .setFriendsOnly(post.isFriendsOnly)
            .setCommentsCount(post.commentsCount)
            .setCanPostComment(post.isCanPostComment)
            .setLikesCount(post.likesCount)
            .setUserLikes(post.isUserLikes)
            .setCanLike(post.isCanLike)
            .setCanEdit(post.isCanEdit)
            .setCanPublish(post.isCanRepost)
            .setRepostCount(post.repostCount)
            .setUserReposted(post.isUserReposted)
            .setPostType(post.postType)
            .setAttachmentsCount(post.attachments?.size().orZero())
            .setSignedId(post.signerId)
            .setCreatedBy(post.creatorId)
            .setCanPin(post.isCanPin)
            .setPinned(post.isPinned)
            .setDeleted(post.isDeleted)
            .setViews(post.viewCount)
            .setDbid(post.dbid)
        val source = post.source
        if (source != null) {
            dbo.setSource(
                SourceDbo().set(
                    source.getType(),
                    source.getPlatform(),
                    source.getData(),
                    source.getUrl()
                )
            )
        }
        dbo.setAttachments(post.attachments?.let { buildEntityAttachments(it) })
        dbo.setCopyHierarchy(mapAll(
            post.getCopyHierarchy()
        ) { buildPostDbo(it) })
        return dbo
    }

    private fun buildVideoDbo(video: Video): VideoDboEntity {
        return VideoDboEntity().set(video.id, video.ownerId)
            .setAlbumId(video.albumId)
            .setTitle(video.title)
            .setDescription(video.description)
            .setLink(video.link)
            .setDate(video.date)
            .setAddingDate(video.addingDate)
            .setViews(video.views)
            .setPlayer(video.player)
            .setImage(video.image)
            .setAccessKey(video.accessKey)
            .setCommentsCount(video.commentsCount)
            .setUserLikes(video.isUserLikes)
            .setLikesCount(video.likesCount)
            .setMp4link240(video.mp4link240)
            .setMp4link360(video.mp4link360)
            .setMp4link480(video.mp4link480)
            .setMp4link720(video.mp4link720)
            .setMp4link1080(video.mp4link1080)
            .setMp4link1440(video.mp4link1440)
            .setMp4link2160(video.mp4link2160)
            .setExternalLink(video.externalLink)
            .setPlatform(video.platform)
            .setRepeat(video.isRepeat)
            .setDuration(video.duration)
            .setPrivacyView(video.privacyView?.let { mapPrivacy(it) })
            .setPrivacyComment(video.privacyComment?.let { mapPrivacy(it) })
            .setCanEdit(video.isCanEdit)
            .setCanAdd(video.isCanAdd)
            .setCanComment(video.isCanComment)
            .setCanRepost(video.isCanRepost)
            .setPrivate(video.private)
            .setFavorite(video.isFavorite)
    }

    private fun mapPrivacy(privacy: SimplePrivacy): PrivacyEntity {
        return PrivacyEntity().set(
            privacy.getType(),
            mapAll(privacy.getEntries()) { orig ->
                PrivacyEntity.Entry().set(orig.getType(), orig.getId(), orig.isAllowed())
            })
    }

    private fun mapAudio(message: VoiceMessage): AudioMessageDboEntity {
        return AudioMessageDboEntity().set(message.getId(), message.getOwnerId())
            .setWaveform(message.getWaveform())
            .setLinkOgg(message.getLinkOgg())
            .setLinkMp3(message.getLinkMp3())
            .setDuration(message.getDuration())
            .setAccessKey(message.getAccessKey())
            .setTranscript(message.getTranscript())
            .setWasListened(message.wasListened())
    }

    private fun buildDocumentDbo(document: Document): DocumentDboEntity {
        val dbo = DocumentDboEntity().set(document.id, document.ownerId)
            .setTitle(document.title)
            .setSize(document.size)
            .setExt(document.ext)
            .setUrl(document.url)
            .setDate(document.date)
            .setType(document.type)
            .setAccessKey(document.accessKey)
        document.graffiti.requireNonNull {
            dbo.setGraffiti(GraffitiDbo().set(it.src, it.width, it.height))
        }
        document.videoPreview.requireNonNull {
            dbo.setVideo(VideoPreviewDbo().set(it.src, it.width, it.height, it.fileSize))
        }
        return dbo
    }

    private fun buildStickerEntity(sticker: Sticker): StickerDboEntity {
        return StickerDboEntity().setId(sticker.id)
            .setImagesWithBackground(mapAll(sticker.imagesWithBackground) {
                map(
                    it
                )
            })
            .setImages(mapAll(sticker.images) {
                map(
                    it
                )
            })
            .setAnimations(mapAll(sticker.animations) {
                mapStickerAnimation(
                    it
                )
            })
            .setAnimationUrl(sticker.animationUrl)
    }

    fun map(image: Sticker.Image): StickerDboEntity.Img {
        return StickerDboEntity.Img().set(image.url, image.width, image.height)
    }

    private fun mapStickerAnimation(dto: Sticker.Animation): AnimationEntity {
        return AnimationEntity().set(dto.url, dto.type)
    }

    private fun buildAudioEntity(audio: Audio): AudioDboEntity {
        return AudioDboEntity().set(audio.id, audio.ownerId)
            .setArtist(audio.artist)
            .setTitle(audio.title)
            .setDuration(audio.duration)
            .setUrl(audio.url)
            .setLyricsId(audio.lyricsId)
            .setAlbumId(audio.albumId)
            .setAlbum_owner_id(audio.album_owner_id)
            .setAlbum_access_key(audio.album_access_key)
            .setGenre(audio.genre)
            .setAccessKey(audio.accessKey)
            .setAlbum_title(audio.album_title)
            .setThumb_image_big(audio.thumb_image_big)
            .setThumb_image_little(audio.thumb_image_little)
            .setThumb_image_very_big(audio.thumb_image_very_big)
            .setIsHq(audio.isHq)
            .setMain_artists(audio.main_artists)
    }

    private fun buildAudioPlaylistEntity(dto: AudioPlaylist): AudioPlaylistDboEntity {
        return AudioPlaylistDboEntity()
            .setId(dto.getId())
            .setOwnerId(dto.getOwnerId())
            .setAccess_key(dto.getAccess_key())
            .setArtist_name(dto.getArtist_name())
            .setCount(dto.getCount())
            .setDescription(dto.getDescription())
            .setGenre(dto.getGenre())
            .setYear(dto.getYear())
            .setTitle(dto.getTitle())
            .setThumb_image(dto.getThumb_image())
            .setUpdate_time(dto.getUpdate_time())
            .setOriginal_access_key(dto.getOriginal_access_key())
            .setOriginal_id(dto.getOriginal_id())
            .setOriginal_owner_id(dto.getOriginal_owner_id())
    }

    private fun buildPhotoEntity(photo: Photo): PhotoDboEntity {
        return PhotoDboEntity().set(photo.getObjectId(), photo.ownerId)
            .setAlbumId(photo.albumId)
            .setWidth(photo.width)
            .setHeight(photo.height)
            .setText(photo.text)
            .setDate(photo.date)
            .setUserLikes(photo.isUserLikes)
            .setCanComment(photo.isCanComment)
            .setLikesCount(photo.likesCount)
            .setRepostsCount(photo.repostsCount)
            .setCommentsCount(photo.commentsCount)
            .setTagsCount(photo.tagsCount)
            .setAccessKey(photo.accessKey)
            .setPostId(photo.postId)
            .setDeleted(photo.isDeleted)
            .setSizes(photo.sizes?.let { buildPhotoSizeEntity(it) })
    }

    private fun buildPhotoAlbumEntity(album: PhotoAlbum): PhotoAlbumDboEntity {
        return PhotoAlbumDboEntity().set(album.getObjectId(), album.ownerId)
            .setSize(album.getSize())
            .setTitle(album.getTitle())
            .setDescription(album.getDescription())
            .setCanUpload(album.isCanUpload())
            .setUpdatedTime(album.getUpdatedTime())
            .setCreatedTime(album.getCreatedTime())
            .setSizes(album.getSizes()?.let { buildPhotoSizeEntity(it) })
            .setPrivacyView(album.getPrivacyView()?.let { mapPrivacy(it) })
            .setPrivacyComment(album.getPrivacyComment()?.let { mapPrivacy(it) })
            .setUploadByAdminsOnly(album.isUploadByAdminsOnly())
            .setCommentsDisabled(album.isCommentsDisabled())
    }

    private fun model2entityNullable(size: PhotoSizes.Size?): PhotoSizeEntity.Size? {
        return if (size != null) {
            PhotoSizeEntity.Size()
                .setUrl(size.url)
                .setW(size.getW())
                .setH(size.getH())
        } else null
    }

    private fun buildPhotoSizeEntity(sizes: PhotoSizes): PhotoSizeEntity {
        return PhotoSizeEntity()
            .setS(model2entityNullable(sizes.getS()))
            .setM(model2entityNullable(sizes.getM()))
            .setX(model2entityNullable(sizes.getX()))
            .setO(model2entityNullable(sizes.getO()))
            .setP(model2entityNullable(sizes.getP()))
            .setQ(model2entityNullable(sizes.getQ()))
            .setR(model2entityNullable(sizes.getR()))
            .setY(model2entityNullable(sizes.getY()))
            .setZ(model2entityNullable(sizes.getZ()))
            .setW(model2entityNullable(sizes.getW()))
    }
}