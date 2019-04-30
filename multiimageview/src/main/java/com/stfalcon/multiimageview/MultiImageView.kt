/*******************************************************************************
 * Copyright 2016 stfalcon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.stfalcon.multiimageview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.widget.ImageView
import java.util.*

/**
 * Created by Anton Bevza on 12/22/16.
 *
 * Modified by DaeJunLee on 04/30/19.
 */

class MultiImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ImageView(context, attrs) {
    //Types of shape
    enum class Shape {
        CIRCLE, RECTANGLE, NONE
    }

    // Types of Shape's position
    enum class ShapePosition {
        LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
    }

    //Shape of view
    var shape = Shape.NONE
        set(value) {
            field = value
            invalidate()
        }
    //Corners radius for rectangle shape
    var rectCorners = 30

    data class CustomAttribute(
            val separateLine: Boolean,
            val color: Int,
            val size: Int)

    private val bitmaps = ArrayList<Bitmap>()
    private val path = Path()
    private val rect = RectF()
    private var multiDrawable: Drawable? = null

    private val customAttribute: CustomAttribute

    init {
        customAttribute = MultiImageView(attrs)
    }

    private fun MultiImageView(attrs: AttributeSet?) : CustomAttribute {
        return if (attrs != null) {

            val types = context.obtainStyledAttributes(attrs, R.styleable.MultiImageView)
            val separateLine = types.getBoolean(R.styleable.MultiImageView_separateLine, false)
            val color = types.getColor(R.styleable.MultiImageView_separateColor, Color.WHITE)
            val size = types.getDimension(R.styleable.MultiImageView_separateSize, 0.0f).toInt()

            CustomAttribute(separateLine, color, size)
        } else {
            CustomAttribute(false, 0, 0)
        }
    }

    /**
     * Add image to view
     */
    fun addImage(bitmap: Bitmap) {
        bitmaps.add(bitmap)
        refresh()
    }

    /**
     * Remove all images
     */
    fun clear() {
        bitmaps.clear()
        refresh()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refresh()
    }

    /**
     * recreate MultiDrawable and set it as Drawable to ImageView
     */
    private fun refresh() {
        multiDrawable = MultiDrawable(context, bitmaps, customAttribute)
        setImageDrawable(multiDrawable)
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas != null) {
            if (drawable != null) {
                //if shape not set - just draw
                if (shape != Shape.NONE) {
                    path.reset()
                    //ImageView size
                    rect.set(0f, 0f, width.toFloat(), height.toFloat())

                    if (shape == Shape.RECTANGLE) {
                        //Rectangle with corners
                        path.addRoundRect(rect, rectCorners.toFloat(),
                                rectCorners.toFloat(), Path.Direction.CW)
                    } else {
                        //Oval
                        path.addOval(rect, Path.Direction.CW)
                    }

                    // TODO : get color from xml and drawing the oval but it it not working.. [START]
                    /*val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    paint.color = customAttribute.color
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    canvas.drawPaint(paint)*/
                    // TODO : get color from xml and drawing the oval but it it not working.. [NED]
                    //Clip with shape
                    canvas.clipPath(path)
                }
                super.onDraw(canvas)
            }
        }
    }
}

class MultiDrawable(val context: Context, val bitmaps: ArrayList<Bitmap>, val customAttribute: MultiImageView.CustomAttribute) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val items = ArrayList<PhotoItem>()

    /**
     * Create PhotoItem with position and size depends of count of images
     */
    private fun init() {
        items.clear()

        // FIXME : Build.VERSION_CODES.O
        if (Build.VERSION.SDK_INT >= 26) {
            drawOreo()
        } else {
            draw()
        }
    }

    // FIXME : Not correct the image's size because of Rect(left, top, right, bottom) so fixed the Rect
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun drawOreo() {
        val oriWidth = bounds.width()
        val oriHeight = bounds.height()

        val widthDivider = bounds.width() / 2
        val heightDivider = bounds.height() / 2

        var separateSize = if (customAttribute.separateLine) (customAttribute.size / 2) else 0

        if (bitmaps.size == 1) {
            val bitmap = scaleCenterCrop(bitmaps[0], oriWidth, oriHeight)

            items.add(PhotoItem(bitmap, Rect(0, 0, oriWidth, oriHeight)))
        } else if (bitmaps.size == 2) {
            val bitmap1 = scaleCenterCrop(bitmaps[0], widthDivider, oriHeight)
            val bitmap2 = scaleCenterCrop(bitmaps[1], widthDivider, oriHeight)

            // left
            items.add(PhotoItem(bitmap1, Rect(0, 0, oriWidth - separateSize, oriHeight)))
            // right
            items.add(PhotoItem(bitmap2, Rect(widthDivider + separateSize, 0, oriWidth + widthDivider + separateSize, oriHeight)))
        } else if (bitmaps.size == 3) {
            val bitmap1 = scaleCenterCrop(bitmaps[0], widthDivider, oriHeight)
            val bitmap2 = scaleCenterCrop(bitmaps[1], widthDivider, heightDivider)
            val bitmap3 = scaleCenterCrop(bitmaps[2], widthDivider, heightDivider)
            // left
            items.add(PhotoItem(bitmap1, Rect(0, 0, oriWidth - separateSize, oriHeight)))
            // right
            items.add(PhotoItem(bitmap2, Rect(widthDivider + separateSize, 0, oriWidth + widthDivider + separateSize, oriHeight - separateSize)))
            items.add(PhotoItem(bitmap3, Rect(widthDivider + separateSize, heightDivider + separateSize, oriWidth + widthDivider + separateSize, oriHeight + heightDivider + separateSize)))

        } else if (bitmaps.size == 4) {
            val bitmap1 = scaleCenterCrop(bitmaps[0], widthDivider, heightDivider)
            val bitmap2 = scaleCenterCrop(bitmaps[1], widthDivider, heightDivider)
            val bitmap3 = scaleCenterCrop(bitmaps[2], widthDivider, heightDivider)
            val bitmap4 = scaleCenterCrop(bitmaps[3], widthDivider, heightDivider)

            // left
            items.add(PhotoItem(bitmap1, Rect(0, 0, oriWidth - separateSize, oriHeight - separateSize)))
            items.add(PhotoItem(bitmap2, Rect(0, heightDivider + separateSize, oriWidth - separateSize, oriHeight + heightDivider - separateSize)))

            // right
            items.add(PhotoItem(bitmap3, Rect(widthDivider + separateSize, 0, oriWidth + widthDivider + separateSize, oriHeight - separateSize)))
            items.add(PhotoItem(bitmap4, Rect(widthDivider + separateSize, heightDivider + separateSize, oriWidth + widthDivider + separateSize, oriHeight + heightDivider + separateSize)))

        }
    }

    fun draw() {
        val width = bounds.width()
        val height = bounds.height()

        if (bitmaps.size == 1) {
            val bitmap = scaleCenterCrop(bitmaps[0], width, width)

            items.add(PhotoItem(bitmap, Rect(0, 0, width, width)))
        } else if (bitmaps.size == 2) {
            val bitmap1 = scaleCenterCrop(bitmaps[0], width / 2, width)
            val bitmap2 = scaleCenterCrop(bitmaps[1], width / 2, width)

            items.add(PhotoItem(bitmap1, Rect(0, 0, width / 2, width)))
            items.add(PhotoItem(bitmap2, Rect(width / 2, 0, width, width)))
        } else if (bitmaps.size == 3) {
            val bitmap1 = scaleCenterCrop(bitmaps[0], width / 2, width)
            val bitmap2 = scaleCenterCrop(bitmaps[1], width / 2, width / 2)
            val bitmap3 = scaleCenterCrop(bitmaps[2], width / 2, width / 2)

            items.add(PhotoItem(bitmap1, Rect(0, 0, width / 2, width)))
            items.add(PhotoItem(bitmap2, Rect(width / 2, 0, width, width / 2)))
            items.add(PhotoItem(bitmap3, Rect(width / 2, width / 2, width, width)))
        } else if (bitmaps.size > 4) {
            val bitmap1 = scaleCenterCrop(bitmaps[0], width / 2, width / 2)
            val bitmap2 = scaleCenterCrop(bitmaps[1], width / 2, width / 2)
            val bitmap3 = scaleCenterCrop(bitmaps[2], width / 2, width / 2)
            val bitmap4 = scaleCenterCrop(bitmaps[3], width / 2, width / 2)

            items.add(PhotoItem(bitmap1, Rect(0, 0, width / 2, width / 2)))
            items.add(PhotoItem(bitmap2, Rect(0, width / 2, width / 2, width)))
            items.add(PhotoItem(bitmap3, Rect(width / 2, 0, width, width / 2)))
            items.add(PhotoItem(bitmap4, Rect(width / 2, width / 2, width, width)))
        }
    }

    override fun draw(canvas: Canvas?) {
        if (canvas != null) {
            items.forEach {
                canvas.drawBitmap(it.bitmap, bounds, it.rect, paint)
            }
        }
    }

    /**
     * scale and center crop image
     */
    private fun scaleCenterCrop(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return ThumbnailUtils.extractThumbnail(bitmap, newWidth, newHeight)
    }

    /***
     * Data class for store bitmap and rect
     */
    data class PhotoItem(val bitmap: Bitmap, val rect: Rect)


    //***Needed to override***//
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun onBoundsChange(rect: Rect) {
        super.onBoundsChange(rect)
        init()
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter) {
        paint.colorFilter = colorFilter
    }
    //***------------------***//
}
