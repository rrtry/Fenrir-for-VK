package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class VoiceMessage : AbsModel {
    private val id: Int
    private val ownerId: Int
    private var duration = 0
    private var waveform: ByteArray? = null
    private var linkOgg: String? = null
    private var linkMp3: String? = null
    private var accessKey: String? = null
    private var showTranscript = false
    private var transcript: String? = null
    private var was_listened = false

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        duration = `in`.readInt()
        waveform = `in`.createByteArray()
        linkOgg = `in`.readString()
        linkMp3 = `in`.readString()
        accessKey = `in`.readString()
        transcript = `in`.readString()
        showTranscript = `in`.readByte().toInt() != 0
    }

    fun setWasListened(listened: Boolean): VoiceMessage {
        this.was_listened = listened
        return this
    }

    fun wasListened(): Boolean {
        return was_listened
    }

    fun isShowTranscript(): Boolean {
        return showTranscript
    }

    fun setShowTranscript(showTranscript: Boolean): VoiceMessage {
        this.showTranscript = showTranscript
        return this
    }

    fun getId(): Int {
        return id
    }

    fun getOwnerId(): Int {
        return ownerId
    }

    fun getAccessKey(): String? {
        return accessKey
    }

    fun setAccessKey(accessKey: String?): VoiceMessage {
        this.accessKey = accessKey
        return this
    }

    fun getDuration(): Int {
        return duration
    }

    fun setDuration(duration: Int): VoiceMessage {
        this.duration = duration
        return this
    }

    fun getWaveform(): ByteArray? {
        return waveform
    }

    fun setWaveform(waveform: ByteArray?): VoiceMessage {
        this.waveform = waveform
        return this
    }

    fun getLinkOgg(): String? {
        return linkOgg
    }

    fun setLinkOgg(linkOgg: String?): VoiceMessage {
        this.linkOgg = linkOgg
        return this
    }

    fun getLinkMp3(): String? {
        return linkMp3
    }

    fun setLinkMp3(linkMp3: String?): VoiceMessage {
        this.linkMp3 = linkMp3
        return this
    }

    fun getTranscript(): String? {
        return transcript
    }

    fun setTranscript(transcript: String?): VoiceMessage {
        this.transcript = transcript
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeInt(duration)
        parcel.writeByteArray(waveform)
        parcel.writeString(linkOgg)
        parcel.writeString(linkMp3)
        parcel.writeString(accessKey)
        parcel.writeString(transcript)
        parcel.writeByte((if (showTranscript) 1 else 0).toByte())
    }

    companion object CREATOR : Parcelable.Creator<VoiceMessage> {
        override fun createFromParcel(parcel: Parcel): VoiceMessage {
            return VoiceMessage(parcel)
        }

        override fun newArray(size: Int): Array<VoiceMessage?> {
            return arrayOfNulls(size)
        }
    }
}