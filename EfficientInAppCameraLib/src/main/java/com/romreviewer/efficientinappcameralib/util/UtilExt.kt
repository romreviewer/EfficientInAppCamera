package com.romreviewer.efficientinappcameralib.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.databinding.ktx.BuildConfig
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun NavController.navigateSafely(navDirections: NavDirections) {
    try {
        navigate(navDirections)
    } catch (ex: Exception) {
        ex.toString().logTag()
    }
}

fun NavController.navigateSafely(@IdRes resId: Int, args: Bundle? = null) {
    try {
        navigate(resId, args)
    } catch (ex: Exception) {
        ex.toString().logTag()
    }
}

fun String?.logTag() {
    if (BuildConfig.DEBUG)
        this?.let { Log.d("LogTag", it) }
}

fun Context.toastS(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.toastL(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

fun Uri.getCompressedFile(context: Context): File {
    val file = File(context.externalCacheDir, "tmp.jpg")
    file.createNewFile()
    val bitmap = uriToBitmap(context, this)
    val bos = ByteArrayOutputStream()
    val newBitmap = getResizedBitmap(bitmap)
    newBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
    val fos = FileOutputStream(file)
    fos.write(bos.toByteArray())
    fos.flush()
    fos.close()
    return file
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}

private fun getResizedBitmap(image: Bitmap): Bitmap {
    var width = image.width
    var height = image.height
    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 1) {
        width = 1080
        height = (width / bitmapRatio).toInt()
    } else {
        height = 1080
        width = (height * bitmapRatio).toInt()
    }
    return Bitmap.createScaledBitmap(image, width, height, true)
}