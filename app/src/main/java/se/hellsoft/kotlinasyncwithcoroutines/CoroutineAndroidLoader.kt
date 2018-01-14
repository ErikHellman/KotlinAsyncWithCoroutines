package se.hellsoft.kotlinasyncwithcoroutines

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

// Quick & dirty logcat extensions
inline fun <reified T> T.logd(message: () -> String) = Log.d(T::class.simpleName, message())

inline fun <reified T> T.loge(error: Throwable, message: () -> String) = Log.d(T::class.simpleName, message(), error)

internal class CoroutineLifecycleListener(private val deferred: Deferred<*>) : LifecycleObserver {
  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  fun cancelCoroutine() {
    if (!deferred.isCancelled) {
      deferred.cancel()
    }
  }
}

// CoroutineContext running on background threads.
internal val Background = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() * 2, "Loader")

/**
 * Creates a lazily started coroutine that runs <code>loader()</code>.
 * The coroutine is automatically cancelled using the CoroutineLifecycleListener.
 */
fun <T> LifecycleOwner.load(loader: suspend () -> T): Deferred<T> {
  val deferred = async(context = Background, start = CoroutineStart.LAZY) {
    loader()
  }

  lifecycle.addObserver(CoroutineLifecycleListener(deferred))
  return deferred
}

/**
 * Extension function on <code>Deferred<T><code> that creates a launches a coroutine which
 * will call <code>await()</code> and pass the returned value to <code>block()</code>.
 */
infix fun <T> Deferred<T>.then(block: suspend (T) -> Unit): Job {
  return launch(context = UI) {
    try {
      block(this@then.await())
    } catch (e: Exception) {
      // Just log the exception to confirm when we get cancelled (Expect JobCancellationException)
      loge(e) { "Exception in then()!" }
      throw e
    }
  }
}

