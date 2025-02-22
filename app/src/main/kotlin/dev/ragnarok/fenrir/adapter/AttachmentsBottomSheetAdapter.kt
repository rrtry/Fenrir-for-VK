package dev.ragnarok.fenrir.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.adapter.holder.IdentificableHolder
import dev.ragnarok.fenrir.adapter.holder.SharedHolders
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.view.CircleRoadProgress
import java.util.*

class AttachmentsBottomSheetAdapter(
    private val data: List<AttachmentEntry>,
    private val actionListener: ActionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val holders: SharedHolders<EntryHolder> = SharedHolders(false)
    private var nextHolderId = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == VTYPE_BUTTON) {
            return ImagesButtonHolder(inflater.inflate(R.layout.button_add_photo, parent, false))
        }
        val holder =
            EntryHolder(inflater.inflate(R.layout.message_attachments_entry, parent, false))
        holder.attachId(generateHolderId())
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VTYPE_BUTTON -> bindAddPhotoButton(holder as ImagesButtonHolder)
            VTYPE_ENTRY -> bindEntryHolder(holder as EntryHolder, position)
        }
    }

    private fun bindEntryHolder(holder: EntryHolder, position: Int) {
        val dataPosition = position - 1
        holder.image.setBackgroundResource(R.drawable.background_unknown_image)
        val entry = data[dataPosition]
        holders.put(entry.id, holder)
        when (val model = entry.attachment) {
            is Photo -> {
                bindImageHolder(holder, model)
            }
            is Upload -> {
                bindUploading(holder, model)
            }
            is Post -> {
                bindPost(holder, model)
            }
            is Video -> {
                bindVideo(holder, model)
            }
            is FwdMessages -> {
                bindMessages(holder, model)
            }
            is WallReply -> {
                bindWallReplies(holder)
            }
            is Document -> {
                bindDoc(holder, model)
            }
            is Audio -> {
                bindAudio(holder, model)
            }
            is Article -> {
                bindArticle(holder, model)
            }
            is Story -> {
                bindStory(holder, model)
            }
            is Call -> {
                bindCall(holder)
            }
            is NotSupported -> {
                bindNotSupported(holder)
            }
            is Event -> {
                bindEvent(holder, model)
            }
            is Market -> {
                bindMarket(holder, model)
            }
            is MarketAlbum -> {
                bindMarketAlbum(holder, model)
            }
            is AudioArtist -> {
                bindAudioArtist(holder, model)
            }
            is AudioPlaylist -> {
                bindAudioPlaylist(holder, model)
            }
            is Graffiti -> {
                bindGraffiti(holder, model)
            }
            is PhotoAlbum -> {
                bindPhotoAlbum(holder, model)
            }
        }
        holder.buttomRemove.setOnClickListener {
            actionListener.onButtonRemoveClick(
                entry
            )
        }
        holder.Retry.setOnClickListener { actionListener.onButtonRetryClick(entry) }
    }

    private fun bindMessages(holder: EntryHolder, messages: FwdMessages) {
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        holder.image.setBackgroundResource(R.drawable.background_emails)
        if (messages.fwds.nonNullNoEmpty() && messages.fwds.size == 1 && messages.fwds[0].body.nonNullNoEmpty()) {
            holder.title.text = AppTextUtils.reduceText(messages.fwds[0].body, 20)
        } else {
            holder.title.setText(R.string.messages)
        }
        bindImageView(holder, null)
    }

    private fun bindWallReplies(holder: EntryHolder) {
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        holder.image.setBackgroundResource(R.drawable.background_emails)
        holder.title.setText(R.string.comment)
        bindImageView(holder, null)
    }

    private fun bindImageView(holder: EntryHolder, url: String?) {
        if (url.isNullOrEmpty()) {
            with().cancelRequest(holder.image)
            holder.image.setImageResource(R.drawable.background_gray)
        } else {
            with()
                .load(url)
                .placeholder(R.drawable.background_gray)
                .into(holder.image)
        }
    }

    private fun bindPhotoAlbum(holder: EntryHolder, album: PhotoAlbum) {
        holder.title.setText(R.string.photo_album)
        val photoLink = album.getSizes()?.getUrlForSize(
            PhotoSize.X,
            false
        )
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        bindImageView(holder, photoLink)
    }

    private fun bindGraffiti(holder: EntryHolder, graffiti: Graffiti) {
        holder.title.setText(R.string.graffity)
        val photoLink = graffiti.url
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        bindImageView(holder, photoLink)
    }

    private fun bindArticle(holder: EntryHolder, article: Article) {
        holder.title.setText(R.string.article)
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        val photoLink = article.photo?.getUrlForSize(
            PhotoSize.X,
            false
        )
        bindImageView(holder, photoLink)
    }

    private fun bindMarket(holder: EntryHolder, market: Market) {
        holder.title.text = market.title
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        bindImageView(holder, market.thumb_photo)
    }

    private fun bindMarketAlbum(holder: EntryHolder, market_album: MarketAlbum) {
        holder.title.text = market_album.getTitle()
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        val photoLink = market_album.getPhoto()?.getUrlForSize(
            PhotoSize.X,
            false
        )
        bindImageView(holder, photoLink)
    }

    private fun bindAudioArtist(holder: EntryHolder, artist: AudioArtist) {
        holder.title.text = artist.getName()
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        val photoLink = artist.getMaxPhoto()
        bindImageView(holder, photoLink)
    }

    private fun bindAudioPlaylist(holder: EntryHolder, link: AudioPlaylist) {
        holder.title.text = link.getTitle()
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        val photoLink = link.getThumb_image()
        bindImageView(holder, photoLink)
    }

    private fun bindStory(holder: EntryHolder, story: Story) {
        holder.title.setText(R.string.story)
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        val photoLink = story.owner?.maxSquareAvatar
        bindImageView(holder, photoLink)
    }

    private fun bindCall(holder: EntryHolder) {
        holder.title.setText(R.string.call)
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        with().cancelRequest(holder.image)
        holder.image.setImageResource(R.drawable.phone_call_color)
    }

    private fun bindNotSupported(holder: EntryHolder) {
        holder.title.setText(R.string.not_supported)
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        with().cancelRequest(holder.image)
        holder.image.setImageResource(R.drawable.not_supported)
    }

    private fun bindEvent(holder: EntryHolder, event: Event) {
        holder.title.text = event.button_text
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        with().cancelRequest(holder.image)
    }

    private fun bindAudio(holder: EntryHolder, audio: Audio) {
        val audiostr = audio.artist + " - " + audio.title
        holder.title.text = audiostr
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        holder.image.setBackgroundResource(R.drawable.background_unknown_song)
        bindImageView(holder, audio.thumb_image_big)
    }

    private fun bindVideo(holder: EntryHolder, video: Video) {
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        holder.title.text = video.title
        bindImageView(holder, video.image)
    }

    private fun bindDoc(holder: EntryHolder, doc: Document) {
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        holder.title.text = doc.title
        val imgUrl = doc.getPreviewWithSize(PhotoSize.Q, false)
        bindImageView(holder, imgUrl)
    }

    private fun bindPost(holder: EntryHolder, post: Post) {
        holder.progress.visibility = View.INVISIBLE
        holder.Retry.visibility = View.GONE
        holder.tintView.visibility = View.GONE
        val title = post.textCopiesInclude
        if (title.isNullOrEmpty()) {
            holder.title.setText(R.string.attachment_wall_post)
        } else {
            holder.title.text = title
        }
        val imgUrl = post.findFirstImageCopiesInclude(PhotoSize.Q, false)
        bindImageView(holder, imgUrl)
    }

    private fun bindUploading(holder: EntryHolder, upload: Upload) {
        holder.tintView.visibility = View.VISIBLE
        val inProgress = upload.status == Upload.STATUS_UPLOADING
        holder.progress.visibility =
            if (inProgress) View.VISIBLE else View.INVISIBLE
        if (inProgress) {
            holder.progress.changePercentage(upload.progress)
        } else {
            holder.progress.changePercentage(0)
        }
        @ColorInt var titleColor = CurrentTheme.getPrimaryTextColorCode(holder.title.context)
        holder.Retry.visibility = View.GONE
        when (upload.status) {
            Upload.STATUS_UPLOADING -> {
                val precentText = upload.progress.toString() + "%"
                holder.title.text = precentText
            }
            Upload.STATUS_CANCELLING -> holder.title.setText(R.string.cancelling)
            Upload.STATUS_QUEUE -> holder.title.setText(R.string.in_order)
            Upload.STATUS_ERROR -> {
                holder.title.setText(R.string.error)
                titleColor = ERROR_COLOR
                holder.Retry.visibility = View.VISIBLE
            }
        }
        holder.title.setTextColor(titleColor)
        if (upload.hasThumbnail()) {
            with()
                .load(upload.buildThumnailUri())
                .placeholder(R.drawable.background_gray)
                .into(holder.image)
        } else {
            with().cancelRequest(holder.image)
            holder.image.setImageResource(R.drawable.background_gray)
        }
    }

    fun changeUploadProgress(id: Int, progress: Int, smoothly: Boolean) {
        val holder = holders.findOneByEntityId(id)
        if (holder != null) {
            val precentText = "$progress%"
            holder.title.text = precentText
            if (smoothly) {
                holder.progress.changePercentageSmoothly(progress)
            } else {
                holder.progress.changePercentage(progress)
            }
        }
    }

    private fun bindImageHolder(holder: EntryHolder, photo: Photo) {
        val url = photo.getUrlForSize(PhotoSize.Q, false)
        holder.Retry.visibility = View.GONE
        holder.progress.visibility = View.INVISIBLE
        holder.tintView.visibility = View.GONE
        holder.title.setText(R.string.photo)
        bindImageView(holder, url)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VTYPE_BUTTON else VTYPE_ENTRY
    }

    private fun bindAddPhotoButton(holder: ImagesButtonHolder) {
        holder.button.setOnClickListener { actionListener.onAddPhotoButtonClick() }
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    private fun generateHolderId(): Int {
        nextHolderId++
        return nextHolderId
    }

    interface ActionListener : EventListener {
        fun onAddPhotoButtonClick()
        fun onButtonRemoveClick(entry: AttachmentEntry)
        fun onButtonRetryClick(entry: AttachmentEntry)
    }

    private class ImagesButtonHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val button: View = itemView.findViewById(R.id.content_root)
    }

    private inner class EntryHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), IdentificableHolder {
        val image: ImageView = itemView.findViewById(R.id.image)
        val title: TextView = itemView.findViewById(R.id.title)
        val buttomRemove: ViewGroup = itemView.findViewById(R.id.progress_root)
        val progress: CircleRoadProgress = itemView.findViewById(R.id.progress_view)
        val Retry: ImageView = itemView.findViewById(R.id.retry_upload)
        val tintView: View = itemView.findViewById(R.id.tint_view)
        override val holderId: Int
            get() = tintView.tag as Int

        fun attachId(id: Int) {
            tintView.tag = id
        }

        init {
            itemView.tag = generateHolderId()
        }
    }

    companion object {
        private val ERROR_COLOR = Color.parseColor("#ff0000")
        private const val VTYPE_BUTTON = 0
        private const val VTYPE_ENTRY = 1
    }

}