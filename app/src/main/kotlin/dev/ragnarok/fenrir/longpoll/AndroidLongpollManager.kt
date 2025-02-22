package dev.ragnarok.fenrir.longpoll

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.SparseArray
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.realtime.IRealtimeMessagesProcessor
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class AndroidLongpollManager internal constructor(
    private val networker: INetworker,
    private val messagesProcessor: IRealtimeMessagesProcessor
) : ILongpollManager, UserLongpoll.Callback, GroupLongpoll.Callback {
    private val map: SparseArray<LongpollEntry> = SparseArray(1)
    private val keepAlivePublisher: PublishProcessor<Int> = PublishProcessor.create()
    private val actionsPublisher: PublishProcessor<VkApiLongpollUpdates> = PublishProcessor.create()
    private val lock = Any()
    private val compositeDisposable = CompositeDisposable()
    override fun observe(): Flowable<VkApiLongpollUpdates> {
        return actionsPublisher.onBackpressureBuffer()
    }

    override fun observeKeepAlive(): Flowable<Int> {
        return keepAlivePublisher.onBackpressureBuffer()
    }

    private fun createLongpoll(accountId: Int): ILongpoll {
        //return accountId > 0 ? new UserLongpoll(networker, accountId, this) : new GroupLongpoll(networker, Math.abs(accountId), this);
        return UserLongpoll(networker, accountId, this)
    }

    override fun forceDestroy(accountId: Int) {
        d(TAG, "forceDestroy, accountId: $accountId")
        synchronized(lock) {
            val entry = map[accountId]
            entry?.destroy()
        }
    }

    override fun keepAlive(accountId: Int) {
        d(TAG, "keepAlive, accountId: $accountId")
        synchronized(lock) {
            var entry = map[accountId]
            if (entry != null) {
                entry.deferDestroy()
            } else {
                entry = LongpollEntry(createLongpoll(accountId), this)
                map.put(accountId, entry)
                entry.connect()
            }
        }
    }

    private fun notifyDestroy(entry: LongpollEntry) {
        d(TAG, "destroyed, accountId: " + entry.accountId)
        synchronized(lock) { map.remove(entry.accountId) }
    }

    private fun notifyPreDestroy(entry: LongpollEntry) {
        d(TAG, "pre-destroy, accountId: " + entry.accountId)
        keepAlivePublisher.onNext(entry.accountId)
    }

    override fun onUpdates(aid: Int, updates: VkApiLongpollUpdates) {
        d(TAG, "updates, accountId: $aid")
        updates.add_message_updates.nonNullNoEmpty {
            messagesProcessor.process(aid, it)
        }
        compositeDisposable.add(
            LongPollEventSaver()
                .save(aid, updates)
                .subscribeOn(MONO_SCHEDULER)
                .observeOn(provideMainThreadScheduler())
                .subscribe({ onUpdatesSaved(updates) }, ignore())
        )
    }

    private fun onUpdatesSaved(updates: VkApiLongpollUpdates) {
        actionsPublisher.onNext(updates)
    }

    override fun onUpdates(groupId: Int, updates: VkApiGroupLongpollUpdates) {}
    private class LongpollEntry(
        val longpoll: ILongpoll,
        manager: AndroidLongpollManager
    ) {
        val handler: SocketHandler = SocketHandler(this)
        val managerReference: WeakReference<AndroidLongpollManager> = WeakReference(manager)
        val accountId: Int = longpoll.accountId
        var released = false
        fun connect() {
            longpoll.connect()
            handler.restartPreDestroy()
        }

        fun destroy() {
            handler.release()
            longpoll.shutdown()
            released = true
            val manager = managerReference.get()
            manager?.notifyDestroy(this)
        }

        fun deferDestroy() {
            handler.restartPreDestroy()
        }

        fun firePreDestroy() {
            val manager = managerReference.get()
            manager?.notifyPreDestroy(this)
        }

    }

    private class SocketHandler(holder: LongpollEntry) :
        Handler(Looper.getMainLooper()) {
        val reference: WeakReference<LongpollEntry> = WeakReference(holder)
        fun restartPreDestroy() {
            removeMessages(PRE_DESTROY)
            removeMessages(DESTROY)
            sendEmptyMessageDelayed(PRE_DESTROY, 30000L)
        }

        fun postDestroy() {
            sendEmptyMessageDelayed(DESTROY, 30000L)
        }

        fun release() {
            removeMessages(PRE_DESTROY)
            removeMessages(DESTROY)
        }

        override fun handleMessage(msg: Message) {
            val holder = reference.get()
            if (holder != null && !holder.released) {
                when (msg.what) {
                    PRE_DESTROY -> {
                        postDestroy()
                        holder.firePreDestroy()
                    }
                    DESTROY -> holder.destroy()
                }
            }
        }

        companion object {
            const val PRE_DESTROY = 2
            const val DESTROY = 3
        }

    }

    companion object {
        private val TAG = AndroidLongpollManager::class.java.simpleName
        private val MONO_SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(1))
    }

}