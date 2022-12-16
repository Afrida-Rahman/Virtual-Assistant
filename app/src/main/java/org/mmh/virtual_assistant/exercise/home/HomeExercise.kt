package org.mmh.virtual_assistant.exercise.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.RawRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mmh.virtual_assistant.R
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.ExerciseData
import org.mmh.virtual_assistant.api.request.ExerciseRequestPayload
import org.mmh.virtual_assistant.api.response.KeyPointRestrictions
import org.mmh.virtual_assistant.core.AsyncAudioPlayer
import org.mmh.virtual_assistant.core.AudioPlayer
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.Utilities.getIndex
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.pow
import kotlin.math.sqrt

abstract class HomeExercise(
    val context: Context,
    val id: Int,
    var name: String = "",
    val active: Boolean = true,
    var protocolId: Int = 0,
    var instruction: String? = "",
    var videoUrls: String = "",
    var imageUrls: List<String> = listOf(),
    var maxRepCount: Int = 0,
    var maxSetCount: Int = 0
) {

    companion object {
        private const val SET_INTERVAL = 7000L
        private const val PHASE_EASE_TIME = 3000L
    }

    open var phaseIndex = 0
    open var rightCountPhases = mutableListOf<Phase>()
    open var wrongStateIndex = 0
    private val audioPlayer = AudioPlayer(context)
    private var setCounter = 0
    private var wrongCounter = 0
    private var repetitionCounter = 0
    private var lastTimePlayed: Int = System.currentTimeMillis().toInt()
    private var nextConstraintCheckTime = System.currentTimeMillis() + 1000L
    private var allConstraintSatisfied = false
    private var focalLengths: FloatArray? = null
    private var previousCountDown = 0
    private var downTimeCounter = 0
    open var phaseEntered = false
    private var phaseEnterTime = System.currentTimeMillis()
    private lateinit var asyncAudioPlayer: AsyncAudioPlayer
    private var takingRest = false

    fun setExercise(
        exerciseName: String,
        exerciseInstruction: String?,
        exerciseImageUrls: List<String>,
        exerciseVideoUrls: String,
        repetitionLimit: Int,
        setLimit: Int,
        protoId: Int
    ) {
        name = exerciseName
        maxRepCount = repetitionLimit
        maxSetCount = setLimit
        protocolId = protoId
        instruction = exerciseInstruction
        imageUrls = exerciseImageUrls
        videoUrls = exerciseVideoUrls
    }

    fun initializeConstraint(tenant: String) {
        val service = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Utilities.getUrl(tenant).getExerciseConstraintsURL)
            .build()
            .create(IExerciseService::class.java)
        val requestPayload = ExerciseRequestPayload(
            Tenant = tenant,
            KeyPointsRestrictions = listOf(
                ExerciseData(id)
            )
        )
        val response = service.getExerciseConstraint(requestPayload)
        response.enqueue(object : Callback<KeyPointRestrictions> {
            override fun onResponse(
                call: Call<KeyPointRestrictions>,
                response: Response<KeyPointRestrictions>
            ) {
                val responseBody = response.body()
                if (responseBody == null || responseBody.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Failed to get necessary constraints for this exercise and got empty response. So, this exercise can't be performed now!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (responseBody[0].KeyPointsRestrictionGroup.isNotEmpty()) {
                        playInstruction(
                            firstDelay = 5000L,
                            firstInstruction = AsyncAudioPlayer.GET_READY,
                            secondDelay = 5000L,
                            secondInstruction = AsyncAudioPlayer.START,
                            shouldTakeRest = true
                        )
                        responseBody[0].KeyPointsRestrictionGroup.forEach { group ->
                            val constraints = mutableListOf<Constraint>()
                            group.KeyPointsRestriction.sortedByDescending { it.Id }
                                .forEach { restriction ->
                                    val constraintType = if (restriction.Scale == "degree") {
                                        ConstraintType.ANGLE
                                    } else {
                                        ConstraintType.LINE
                                    }
                                    val startPointIndex = getIndex(restriction.StartKeyPosition)
                                    val middlePointIndex = getIndex(restriction.MiddleKeyPosition)
                                    val endPointIndex = getIndex(restriction.EndKeyPosition)
                                    when (constraintType) {
                                        ConstraintType.LINE -> {
                                            if (startPointIndex >= 0 && endPointIndex >= 0) {
                                                constraints.add(
                                                    Constraint(
                                                        minValue = restriction.MinValidationValue,
                                                        maxValue = restriction.MaxValidationValue,
                                                        uniqueId = restriction.Id,
                                                        type = constraintType,
                                                        startPointIndex = startPointIndex,
                                                        middlePointIndex = middlePointIndex,
                                                        endPointIndex = endPointIndex,
                                                        clockWise = false
                                                    )
                                                )
                                            }
                                        }
                                        ConstraintType.ANGLE -> {
                                            if (startPointIndex >= 0 && middlePointIndex >= 0 && endPointIndex >= 0) {
                                                constraints.add(
                                                    Constraint(
                                                        minValue = restriction.MinValidationValue,
                                                        maxValue = restriction.MaxValidationValue,
                                                        uniqueId = restriction.Id,
                                                        type = constraintType,
                                                        startPointIndex = startPointIndex,
                                                        middlePointIndex = middlePointIndex,
                                                        endPointIndex = endPointIndex,
                                                        clockWise = restriction.AngleArea == "clockwise"
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            rightCountPhases.add(
                                Phase(
                                    phaseNumber = group.Phase,
                                    constraints = constraints,
                                    holdTime = group.HoldInSeconds,
                                    phaseDialogue = group.PhaseDialogue
                                )
                            )
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Don't have enough data to perform this exercise. Please provide details of this exercise using EMMA LPT app!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                rightCountPhases = sortedPhaseList(rightCountPhases.toList()).toMutableList()
            }

            override fun onFailure(call: Call<KeyPointRestrictions>, t: Throwable) {
                Toast.makeText(
                    context,
                    "Failed to get exercise response from API !!!",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        asyncAudioPlayer = AsyncAudioPlayer(context)
    }

    fun getMaxHoldTime(): Int = rightCountPhases.map { it.holdTime }.maxOrNull() ?: 0

    fun getRepetitionCount() = repetitionCounter

    fun getWrongCount() = wrongCounter

    fun getSetCount() = setCounter

    fun getHoldTimeLimitCount(): Int = downTimeCounter

    fun getPhase(): Phase? {
        return if (phaseIndex < rightCountPhases.size) {
            rightCountPhases[phaseIndex]
        } else {
            null
        }
    }

    fun playInstruction(
        firstDelay: Long,
        firstInstruction: String,
        secondDelay: Long = 0L,
        secondInstruction: String? = null,
        shouldTakeRest: Boolean = false
    ) {
        if (shouldTakeRest) takingRest = true
        CoroutineScope(Dispatchers.Main).launch {
            delay(firstDelay)
            asyncAudioPlayer.playText(firstInstruction)
            delay(secondDelay)
            secondInstruction?.let {
                asyncAudioPlayer.playText(it)
            }
            if (shouldTakeRest) takingRest = false
        }
    }

    fun repetitionCount() {
        repetitionCounter++
        audioPlayer.playFromFile(R.raw.right_count)
        if (repetitionCounter >= maxRepCount) {
            repetitionCounter = 0
            setCounter++
            if (setCounter == maxSetCount) {
                asyncAudioPlayer.playText(AsyncAudioPlayer.FINISH)
                CoroutineScope(Dispatchers.Main).launch {
                    playInstruction(
                        firstDelay = 1000L,
                        firstInstruction = AsyncAudioPlayer.CONGRATS
                    )
                }
            } else {
                playInstruction(
                    firstDelay = 0L,
                    firstInstruction = setCountText(setCounter),
                    secondDelay = SET_INTERVAL,
                    secondInstruction = AsyncAudioPlayer.START_AGAIN,
                    shouldTakeRest = true
                )
            }
        }
    }

    fun wrongCount() {
        wrongCounter++
        audioPlayer.playFromFile(R.raw.wrong_count)
    }

    fun getPersonDistance(person: Person): Float? {
        val pointA = person.keyPoints[BodyPart.LEFT_SHOULDER.position]
        val pointB = person.keyPoints[BodyPart.LEFT_ELBOW.position]
        val distanceInPx = sqrt(
            (pointA.coordinate.x - pointB.coordinate.x).toDouble()
                .pow(2) + (pointA.coordinate.y - pointB.coordinate.y).toDouble().pow(2)
        )
        var sum = 0f
        var distance: Float? = null
        focalLengths?.let {
            focalLengths?.forEach { value ->
                sum += value
            }
            val avgFocalLength = (sum / focalLengths!!.size) * 0.04f
            distance = (avgFocalLength / distanceInPx.toFloat()) * 12 * 3000f
        }
        return distance?.let { it / 12 }
    }

    fun setFocalLength(lengths: FloatArray?) {
        focalLengths = lengths
    }

    fun playAudio(@RawRes resource: Int) {
        val timestamp = System.currentTimeMillis().toInt()
        if (timestamp - lastTimePlayed >= 3500) {
            lastTimePlayed = timestamp
            audioPlayer.playFromFile(resource)
        }
    }

    open fun onEvent(event: CommonInstructionEvent) {
        when (event) {
            is CommonInstructionEvent.OutSideOfBox -> playAudio(R.raw.please_stay_inside_box)
            is CommonInstructionEvent.HandIsNotStraight -> playAudio(R.raw.keep_hand_straight)
            is CommonInstructionEvent.LeftHandIsNotStraight -> playAudio(R.raw.left_hand_straight)
            is CommonInstructionEvent.RightHandIsNotStraight -> playAudio(R.raw.right_hand_straight)
            is CommonInstructionEvent.TooFarFromCamera -> playAudio(R.raw.come_forward)
        }
    }

    open fun rightExerciseCount(
        person: Person,
        canvasHeight: Int,
        canvasWidth: Int
    ) {
        if (rightCountPhases.isNotEmpty() && phaseIndex < rightCountPhases.size && !takingRest) {
            val phase = rightCountPhases[phaseIndex]
            val constraintSatisfied = isConstraintSatisfied(
                person,
                phase.constraints
            )
            Log.d("CountingIssue", "$phaseIndex -- $constraintSatisfied")
            if (VisualizationUtils.isInsideBox(
                    person,
                    canvasHeight,
                    canvasWidth
                ) && constraintSatisfied
            ) {
                if (!phaseEntered) {
                    phaseEntered = true
                    phaseEnterTime = System.currentTimeMillis()
                }
                val elapsedTime = ((System.currentTimeMillis() - phaseEnterTime) / 1000).toInt()
                downTimeCounter = phase.holdTime - elapsedTime
                if (downTimeCounter <= 0) {
                    if (phaseIndex == rightCountPhases.size - 1) {
                        phaseIndex = 0
                        wrongStateIndex = 0
                        repetitionCount()
                    } else {
                        phaseIndex++
                        rightCountPhases[phaseIndex].phaseDialogue?.let {
                            playInstruction(firstDelay = 500L, firstInstruction = it)
                        }
                        downTimeCounter = 0
                    }
                } else {
                    countDownAudio(downTimeCounter)
                }
            } else {
                downTimeCounter = 0
                phaseEntered = false
            }
            commonInstruction(
                person,
                rightCountPhases[phaseIndex].constraints,
                canvasHeight,
                canvasWidth
            )
            instruction(person)
        }
    }

    open fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {}

    open fun instruction(person: Person) {}

    private fun setCountText(count: Int): String = when (count) {
        1 -> AsyncAudioPlayer.SET_1
        2 -> AsyncAudioPlayer.SET_2
        3 -> AsyncAudioPlayer.SET_3
        4 -> AsyncAudioPlayer.SET_4
        5 -> AsyncAudioPlayer.SET_5
        6 -> AsyncAudioPlayer.SET_6
        7 -> AsyncAudioPlayer.SET_7
        8 -> AsyncAudioPlayer.SET_8
        9 -> AsyncAudioPlayer.SET_9
        10 -> AsyncAudioPlayer.SET_10
        else -> AsyncAudioPlayer.SET_COMPLETED
    }

    private fun isConstraintSatisfied(person: Person, constraints: List<Constraint>): Boolean {
        var constraintSatisfied = true
        constraints.forEach {
            when (it.type) {
                ConstraintType.ANGLE -> {
                    val angle = Utilities.angle(
                        startPoint = person.keyPoints[it.startPointIndex].toRealPoint(),
                        middlePoint = person.keyPoints[it.middlePointIndex].toRealPoint(),
                        endPoint = person.keyPoints[it.endPointIndex].toRealPoint(),
                        clockWise = it.clockWise
                    )
                    if (angle < it.minValue || angle > it.maxValue) {
                        Log.d("CountingIssue", "${it.minValue}< $angle < ${it.maxValue}")
                        constraintSatisfied = false
                    }
                }
                ConstraintType.LINE -> {}
            }
        }
        return constraintSatisfied
    }

    private fun sortedPhaseList(phases: List<Phase>): List<Phase> {
        val phaseIndices = mutableListOf<Int>()
        return phases.sortedBy { it.phaseNumber }.filter {
            val shouldAdd = !phaseIndices.contains(it.phaseNumber)
            phaseIndices.add(it.phaseNumber)
            shouldAdd
        }
    }

    private fun commonInstruction(
        person: Person,
        constraints: List<Constraint>,
        canvasHeight: Int,
        canvasWidth: Int
    ) {
        constraints.forEach { _ ->
            if (!VisualizationUtils.isInsideBox(
                    person,
                    canvasHeight,
                    canvasWidth
                )
            ) onEvent(CommonInstructionEvent.OutSideOfBox)
        }
        getPersonDistance(person)?.let {
            if (it > 13) {
                onEvent(CommonInstructionEvent.TooFarFromCamera)
            }
        }
    }

    private fun countDownAudio(count: Int) {
        if (previousCountDown != count && count > 0) {
            previousCountDown = count
            asyncAudioPlayer.playNumber(count)
        }
    }

    sealed class CommonInstructionEvent {
        object OutSideOfBox : CommonInstructionEvent()
        object HandIsNotStraight : CommonInstructionEvent()
        object LeftHandIsNotStraight : CommonInstructionEvent()
        object RightHandIsNotStraight : CommonInstructionEvent()
        object TooFarFromCamera : CommonInstructionEvent()
    }
}