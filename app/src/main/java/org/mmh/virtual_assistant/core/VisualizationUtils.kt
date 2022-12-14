package org.mmh.virtual_assistant.core


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import org.mmh.virtual_assistant.domain.model.BodyPart
import org.mmh.virtual_assistant.domain.model.ConstraintType
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.domain.model.Phase

object VisualizationUtils {
    private const val LINE_WIDTH = 3f
    private const val BORDER_WIDTH = 10f
    private var lastTimeChecked: Long = 0L

    private val MAPPINGS = listOf(
        listOf(BodyPart.LEFT_EAR.position, BodyPart.LEFT_EYE.position),
        listOf(BodyPart.LEFT_EYE.position, BodyPart.NOSE.position),
        listOf(BodyPart.NOSE.position, BodyPart.RIGHT_EYE.position),
        listOf(BodyPart.RIGHT_EYE.position, BodyPart.RIGHT_EAR.position),
        listOf(BodyPart.LEFT_SHOULDER.position, BodyPart.RIGHT_SHOULDER.position),
        listOf(BodyPart.LEFT_SHOULDER.position, BodyPart.LEFT_ELBOW.position),
        listOf(BodyPart.LEFT_ELBOW.position, BodyPart.LEFT_WRIST.position),
        listOf(BodyPart.LEFT_SHOULDER.position, BodyPart.LEFT_HIP.position),
        listOf(BodyPart.LEFT_HIP.position, BodyPart.LEFT_KNEE.position),
        listOf(BodyPart.LEFT_KNEE.position, BodyPart.LEFT_ANKLE.position),
        listOf(BodyPart.LEFT_HIP.position, BodyPart.RIGHT_HIP.position),
        listOf(BodyPart.RIGHT_SHOULDER.position, BodyPart.RIGHT_ELBOW.position),
        listOf(BodyPart.RIGHT_ELBOW.position, BodyPart.RIGHT_WRIST.position),
        listOf(BodyPart.RIGHT_SHOULDER.position, BodyPart.RIGHT_HIP.position),
        listOf(BodyPart.RIGHT_HIP.position, BodyPart.RIGHT_KNEE.position),
        listOf(BodyPart.RIGHT_KNEE.position, BodyPart.RIGHT_ANKLE.position)
    )

    fun drawBodyKeyPoints(
        input: Bitmap,
        person: Person,
        phase: Phase?,
        isFrontCamera: Boolean = false
    ): Bitmap {
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        if (isFrontCamera) {
            canvas.scale(-1f, 1f, canvas.width.toFloat() / 2, canvas.height.toFloat() / 2)
        }
        val draw = Draw(canvas, Color.WHITE, LINE_WIDTH)
        val width = draw.canvas.width
        val height = draw.canvas.height

        MAPPINGS.forEach { map ->
            val startPoint = person.keyPoints[map[0]].toCanvasPoint()
            val endPoint = person.keyPoints[map[1]].toCanvasPoint()
            if (isFrontCamera) {
                draw.line(
                    Point(
                        output.width - startPoint.x,
                        startPoint.y
                    ),
                    Point(
                        output.width - endPoint.x,
                        endPoint.y
                    ),
                    _color = Color.rgb(170, 255, 0)
                )
            } else {
                draw.line(startPoint, endPoint, _color = Color.rgb(170, 255, 0))
            }
        }

        phase?.let {
            if (System.currentTimeMillis() - lastTimeChecked > 1000) {
                for (i in 0 until it.constraints.size) {
                    val constraint = it.constraints[i]
                    if (constraint.type == ConstraintType.ANGLE) {
                        val angle = Utilities.angle(
                            startPoint = person.keyPoints[constraint.startPointIndex].toRealPoint(),
                            middlePoint = person.keyPoints[constraint.middlePointIndex].toRealPoint(),
                            endPoint = person.keyPoints[constraint.endPointIndex].toRealPoint(),
                            clockWise = constraint.clockWise
                        )
                        if (angle >= constraint.minValue && angle <= constraint.maxValue) {
                            it.constraints[i].color = Color.WHITE
                        } else {
                            it.constraints[i].color = Color.RED
                        }
                    }
                }
                lastTimeChecked = System.currentTimeMillis()
            }
            for (constraint in it.constraints) {
                val startPoint = person.keyPoints[constraint.startPointIndex].toCanvasPoint()
                val endPoint = person.keyPoints[constraint.endPointIndex].toCanvasPoint()
                if (constraint.type == ConstraintType.ANGLE) {
                    val middlePoint = person.keyPoints[constraint.middlePointIndex].toCanvasPoint()
                    if (isFrontCamera) {
                        draw.angle(
                            Point(
                                output.width - startPoint.x,
                                startPoint.y
                            ),
                            Point(
                                output.width - middlePoint.x,
                                middlePoint.y
                            ),
                            Point(
                                output.width - endPoint.x,
                                endPoint.y
                            ),
                            _clockWise = !constraint.clockWise,
                            color = constraint.color
                        )
                    } else {
                        draw.angle(
                            startPoint,
                            middlePoint,
                            endPoint,
                            _clockWise = constraint.clockWise,
                            color = constraint.color
                        )
                    }
                } else {
                    if (isFrontCamera) {
                        draw.line(
                            Point(
                                output.width - startPoint.x,
                                startPoint.y
                            ),
                            Point(
                                output.width - endPoint.x,
                                endPoint.y
                            ),
                            _color = constraint.color
                        )
                    } else {
                        draw.line(
                            startPoint,
                            endPoint,
                            _color = constraint.color
                        )
                    }
                }
            }
        }
        if (!isInsideBox(person, height, width)) {
            draw.tetragonal(
                Point(0f, 0f),
                Point(0f, height.toFloat()),
                Point(width.toFloat(), height.toFloat()),
                Point(width.toFloat(), 0f),
                _color = Color.RED,
                _thickness = BORDER_WIDTH
            )
        }
        return output
    }

    fun isInsideBox(person: Person, canvasHeight: Int, canvasWidth: Int): Boolean {
        var rightPosition = true
        person.keyPoints.forEach {
            val x = it.coordinate.x
            val y = it.coordinate.y
            if (x < 0 || x > canvasWidth || y < 0 || y > canvasHeight) {
                rightPosition = false
            }
        }
        return rightPosition
    }

    fun getAlertDialogue(
        context: Context,
        message: String,
        positiveButtonText: String,
        positiveButtonAction: () -> Unit,
        negativeButtonText: String?,
        negativeButtonAction: () -> Unit
    ): AlertDialog.Builder {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton(positiveButtonText) { _, _ ->
            positiveButtonAction()
        }
        negativeButtonText?.let {
            alertDialog.setNegativeButton(it) { _, _ ->
                negativeButtonAction()
            }
        }
        return alertDialog
    }
}
