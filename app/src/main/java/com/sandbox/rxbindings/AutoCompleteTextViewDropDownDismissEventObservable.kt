package com.sandbox.rxbindings

import android.os.Looper
import android.widget.AutoCompleteTextView
import androidx.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.disposables.Disposables

@CheckResult
fun AutoCompleteTextView.dismissEvents(): Observable<Unit> {
  return AutoCompleteTextViewDropDownDismissEventObservable(this)
}

private class AutoCompleteTextViewDropDownDismissEventObservable(
  private val view: AutoCompleteTextView
) : Observable<Unit>() {

  override fun subscribeActual(observer: Observer<in Unit>) {
    if (!checkMainThread(observer)) {
      return
    }
    val listener = Listener(view, observer)
    observer.onSubscribe(listener)
    view.setOnDismissListener(listener)
  }

  private class Listener(
    private val view: AutoCompleteTextView,
    private val observer: Observer<in Unit>
  ) : MainThreadDisposable(), AutoCompleteTextView.OnDismissListener {

    override fun onDismiss() {
      if (isDisposed) return
      observer.onNext(Unit)
    }

    override fun onDispose() = view.setOnDismissListener(null)
  }
}

fun checkMainThread(observer: Observer<*>): Boolean {
  if (Looper.myLooper() != Looper.getMainLooper()) {
    observer.onSubscribe(Disposables.empty())
    observer.onError(
      IllegalStateException(
        "Expected to be called on the main thread but was " + Thread.currentThread().name
      )
    )
    return false
  }
  return true
}