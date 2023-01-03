package org.mmh.virtual_assistant.core

import android.graphics.Bitmap
import android.graphics.RectF

data class VisualOutput(val bitmap: Bitmap, val noRectF: RectF, val yesRectF: RectF)
