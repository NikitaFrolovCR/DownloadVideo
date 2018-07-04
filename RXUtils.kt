package com.nikita_frolov_cr.downloadvideo

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Helper class for RxJava
 */
object RxUtils {

    /**
     * Unsubscribe subscription if it is not null
     * @param subscription some subscription
     */
    fun unsubscribeIfNotNull(subscription: Disposable?) {
        subscription?.dispose()
    }

    /**
     * Returns new Subscription if current is null or unsubscribed
     * @param subscription some subscription
     * @return new Subscription if current is null or unsubscribed
     */
    fun getNewCompositeSubIfUnsubscribed(subscription: CompositeDisposable?) =
            if (subscription == null || subscription.isDisposed) {
                CompositeDisposable()
            } else subscription

    /**
     * ? -> IO -> MAIN
     * Subscribes in the worker IO thread
     * Observes in the Main thread
     * @param observable some observable
     * @param <T> any type; input type = output type
     * @return input observable, which subscribes in the worker IO thread and observes in the Main thread
    </T> */
    fun <T> ioToMain(observable: Flowable<T>): Flowable<T> = observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun <T> ioToMain(observable: Observable<T>): Observable<T> = observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    /**
     * ? -> IO -> IO
     * Subscribes in the worker IO thread
     * Observes in the worker IO thread
     * @param observable some observable
     * @param <T> any type; input type = output type
     * @return input observable, which subscribes in the worker IO thread and observes in the worker IO thread
    </T> */
    fun <T> toWorkerThread(observable: Flowable<T>): Flowable<T> = observable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

    fun <T> ioToMainTransformer() = FlowableTransformer<T, T> { inObservable ->
        inObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun <T> toWorkerThreadTransformer() = FlowableTransformer<T, T> { inObservable ->
        inObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
    }
}