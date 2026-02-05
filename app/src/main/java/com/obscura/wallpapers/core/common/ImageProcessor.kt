package com.obscura.wallpapers.core.common

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.obscura.wallpapers.R

object ImageProcessor {

    fun blurGaussian(
        context: Context,
        bitmap: Bitmap,
        radius: Float = 25f,
        scale: Float = 0.2f
    ): Bitmap {
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val rs = RenderScript.create(context)
        val theScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        theScript.setRadius(radius.coerceIn(0f, 25f))

        val theInAllocation = Allocation.createFromBitmap(rs, inputBitmap)
        val theOutAllocation = Allocation.createFromBitmap(rs, outputBitmap)
        theScript.setInput(theInAllocation)
        theScript.forEach(theOutAllocation)
        theOutAllocation.copyTo(outputBitmap)

        rs.destroy()
        return outputBitmap
    }

    fun drawableIdForName(resourceName: String): Int? {
        return try {
            R.mipmap::class.java.getField(resourceName).getInt(null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
