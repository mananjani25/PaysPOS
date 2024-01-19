package com.pays.pos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun showSignatureInImageView(context: Context, signedBitmap: Bitmap): Bitmap {
        val file = File(context.filesDir, "signature.png")
        val fos = FileOutputStream(file)
        signedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
        fos.close()
        return bmp
    }

}