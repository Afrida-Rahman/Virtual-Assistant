package org.mmh.virtual_assistant.core

import android.graphics.Bitmap
import android.graphics.RectF

data class VizOutput(val bitmap: Bitmap, val noRectF: RectF, val yesRectF: RectF)
