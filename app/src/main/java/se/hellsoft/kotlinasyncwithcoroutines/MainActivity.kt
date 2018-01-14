package se.hellsoft.kotlinasyncwithcoroutines

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.delay

class MainActivity : AppCompatActivity() {
  companion object {
    private const val KEY_IMAGE_URI = "imageUri"
    private const val IMAGE_PICK_REQUEST = 101
  }

  private var imageUri: Uri? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    button.setOnClickListener {
      val intent = Intent(Intent.ACTION_GET_CONTENT).also { it.type = "image/*" }
      startActivityForResult(intent, IMAGE_PICK_REQUEST)
      logd { "Pick a photo from media store." }
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_IMAGE_URI)) {
      logd { "Saved state contains an image Uri. Let's assign it to imageUri." }
      imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI) as Uri
      imageUri?.let { loadAndShowPhoto(it) }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
      logd { "User picked photo with uri ${data.data}, let's load it" }
      imageUri = data.data // So we can store it in onSaveInstanceState
      loadAndShowPhoto(data.data)
    }
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    if (outState != null && imageUri != null) {
      logd { "Let's save the imageUri to our saved state so we can load it when we come back!" }
      outState.putParcelable(KEY_IMAGE_URI, imageUri)
    }
  }

  private fun loadAndShowPhoto(uri: Uri) {
    load {
      // This run on a background thread
      logd { "Start loading image on thread ${Thread.currentThread().name}" }
      delay(5000L) // Fake a long loading so we can test what happens in onStop()
      MediaStore.Images.Media.getBitmap(contentResolver, uri)
    } then {
      // This runs on the main thread
      logd { "Image with size ${it.width} x ${it.height} loaded. Display on ImageView running on thread ${Thread.currentThread().name}" }
      imageView.setImageBitmap(it)
    }
  }
}
