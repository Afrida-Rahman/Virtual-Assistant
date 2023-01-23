package org.mmh.virtual_assistant.exercise.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.RawRes
import kotlinx.coroutines.*
import org.mmh.virtual_assistant.R
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.ExerciseRequestPayload
import org.mmh.virtual_assistant.api.request.PhaseSummary
import org.mmh.virtual_assistant.api.request.Restriction
import org.mmh.virtual_assistant.api.response.KeyPointRestrictions
import org.mmh.virtual_assistant.api.response.PhaseInfo1
import org.mmh.virtual_assistant.core.AsyncAudioPlayer
import org.mmh.virtual_assistant.core.AudioPlayer
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.Utilities.getIndex
import org.mmh.virtual_assistant.core.Utilities.getIndexName
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

abstract class HomeExercise(
    val context: Context,
    val id: Int,
    val playPauseCue: Boolean = true,
    var name: String = "",
    val active: Boolean = true,
    var protocolId: Int = 0,
    var instruction: String? = "",
    var videoUrls: String = "",
    var imageUrls: List<String> = listOf(),
    var maxRepCount: Int = 0,
    var maxSetCount: Int = 0,
    var phaseList: List<PhaseInfo1> = listOf()
) {

    companion object {
        private const val SET_INTERVAL = 7000L
        private const val MAX_DISTANCE_FROM_CAMERA = 13
    }

    open var phaseIndex = 0
    open var rightCountPhases = mutableListOf<Phase>()
    private var phaseSummary = mutableListOf<PhaseSummary>()
    private var restriction = mutableListOf<Restriction>()
    private val audioPlayer = AudioPlayer(context)
    private var setCounter = 0
    private var wrongCounter = 0
    private var repetitionCounter = 0
    private var lastTimePlayed: Int = System.currentTimeMillis().toInt()
    private var focalLengths: FloatArray? = null
    private var previousCountDown = 0
    private var downTimeCounter = 0
    private var distanceFromCamera = 0f
    private var lastTimeDistanceCalculated = 0L
    private val distanceCalculationInterval = 2000L
    open var phaseEntered = false
    private var phaseEnterTime = System.currentTimeMillis()
    private var takingRest = false
    private var manuallyPaused = false
    private lateinit var asyncAudioPlayer: AsyncAudioPlayer
    private val instructions: MutableList<Instruction> = mutableListOf()
    var trackIndex: Int = 0
    private val commonExerciseInstructions = listOf(
        AsyncAudioPlayer.GET_READY,
        AsyncAudioPlayer.START,
        AsyncAudioPlayer.START_AGAIN,
        AsyncAudioPlayer.FINISH,
        AsyncAudioPlayer.ONE,
        AsyncAudioPlayer.TWO,
        AsyncAudioPlayer.THREE,
        AsyncAudioPlayer.FOUR,
        AsyncAudioPlayer.FIVE,
        AsyncAudioPlayer.SIX,
        AsyncAudioPlayer.SEVEN,
        AsyncAudioPlayer.EIGHT,
        AsyncAudioPlayer.NINE,
        AsyncAudioPlayer.TEN,
        AsyncAudioPlayer.ELEVEN,
        AsyncAudioPlayer.TWELVE,
        AsyncAudioPlayer.THIRTEEN,
        AsyncAudioPlayer.FOURTEEN,
        AsyncAudioPlayer.FIFTEEN,
        AsyncAudioPlayer.SIXTEEN,
        AsyncAudioPlayer.SEVENTEEN,
        AsyncAudioPlayer.EIGHTEEN,
        AsyncAudioPlayer.NINETEEN,
        AsyncAudioPlayer.TWENTY,
        AsyncAudioPlayer.BEEP,
        AsyncAudioPlayer.PAUSE,
    )
    val consideredIndices = mutableSetOf<Int>()

    fun addInstruction(dialogue: String?) {
        dialogue?.let { text ->
            val doesNotExist = instructions.find {
                it.text.lowercase() == text.lowercase()
            } == null
            if (doesNotExist) {
                instructions.add(asyncAudioPlayer.generateInstruction(dialogue))
            }
        }
    }

    fun setExercise(
        exerciseName: String,
        exerciseInstruction: String?,
        exerciseImageUrls: List<String>,
        exerciseVideoUrls: String,
        repetitionLimit: Int,
        setLimit: Int,
        protoId: Int,
        phases: List<PhaseInfo1> = emptyList()
    ) {
        name = exerciseName
        maxRepCount = repetitionLimit
        maxSetCount = setLimit
        protocolId = protoId
        instruction = exerciseInstruction
        imageUrls = exerciseImageUrls
        videoUrls = exerciseVideoUrls
        phaseList = phases
    }

    fun initializeConstraint(tenant: String) {
        val service = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(Utilities.getUrl(tenant).getExerciseConstraintsURL)
            .build()
            .create(IExerciseService::class.java)
        val requestPayload = ExerciseRequestPayload(
            Tenant = tenant,
            ExerciseId = id
        )
        val response = service.getExerciseConstraint(requestPayload)
        response.enqueue(object : Callback<KeyPointRestrictions> {
            override fun onResponse(
                call: Call<KeyPointRestrictions>,
                response: Response<KeyPointRestrictions>
            ) {
                val responseBody = response.body()
                if (responseBody == null || responseBody.Phases.isEmpty()) {
                    MainScope().launch {
                        Toast.makeText(
                            context,
                            "Failed to get necessary constraints for this exercise and got empty response. So, this exercise can't be performed now!",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    if (responseBody.Phases.isNotEmpty()) {
                        playInstruction(
                            firstDelay = 5000L,
                            firstInstruction = AsyncAudioPlayer.GET_READY,
                            secondDelay = 5000L,
                            secondInstruction = AsyncAudioPlayer.START,
                            shouldTakeRest = true
                        )
                        asyncAudioPlayer = AsyncAudioPlayer(context)
                        commonExerciseInstructions.forEach {
                            addInstruction(it)
                        }
                        responseBody.Phases.sortedBy { it.PhaseNumber }.forEach { group ->
                            val constraints = mutableListOf<Constraint>()
                            group.Restrictions.forEach { restriction ->
                                val constraintType = if (restriction.Scale == "degree") {
                                    ConstraintType.ANGLE
                                } else {
                                    ConstraintType.LINE
                                }
                                val startPointIndex = getIndex(restriction.StartKeyPosition)
                                val middlePointIndex = getIndex(restriction.MiddleKeyPosition)
                                val endPointIndex = getIndex(restriction.EndKeyPosition)
                                consideredIndices.add(startPointIndex)
                                consideredIndices.add(middlePointIndex)
                                consideredIndices.add(endPointIndex)
                                when (constraintType) {
                                    ConstraintType.LINE -> {
                                        if (startPointIndex >= 0 && endPointIndex >= 0) {
                                            constraints.add(
                                                Constraint(
                                                    type = constraintType,
                                                    startPointIndex = startPointIndex,
                                                    middlePointIndex = middlePointIndex,
                                                    endPointIndex = endPointIndex,
                                                    clockWise = false,
                                                    minValue = restriction.LowestMinValidationValue,
                                                    maxValue = restriction.LowestMaxValidationValue,
                                                    looseMin = restriction.LowestMinValidationValue,
                                                    looseMax = restriction.LowestMaxValidationValue,
                                                    standardMin = restriction.MinValidationValue,
                                                    standardMax = restriction.MaxValidationValue
                                                )
                                            )
                                        }
                                    }
                                    ConstraintType.ANGLE -> {
                                        if (startPointIndex >= 0 && middlePointIndex >= 0 && endPointIndex >= 0) {
                                            constraints.add(
                                                Constraint(
                                                    type = constraintType,
                                                    startPointIndex = startPointIndex,
                                                    middlePointIndex = middlePointIndex,
                                                    endPointIndex = endPointIndex,
                                                    clockWise = restriction.AngleArea == "clockwise",
                                                    minValue = restriction.LowestMinValidationValue,
                                                    maxValue = restriction.LowestMaxValidationValue,
                                                    looseMin = restriction.LowestMinValidationValue,
                                                    looseMax = restriction.LowestMaxValidationValue,
                                                    standardMin = restriction.MinValidationValue,
                                                    standardMax = restriction.MaxValidationValue
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            rightCountPhases.add(
                                Phase(
                                    phaseNumber = group.PhaseNumber,
                                    constraints = constraints,
                                    holdTime = group.HoldInSeconds,
                                    phaseDialogue = group.PhaseDialogue
                                )
                            )
                            addInstruction(group.PhaseDialogue)
                        }
                    } else {
                        MainScope().launch {
                            Toast.makeText(
                                context,
                                "Don't have enough data to perform this exercise. Please provide details of this exercise using EMMA LPT app!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                rightCountPhases = sortedPhaseList(rightCountPhases.toList()).toMutableList()
            }

            override fun onFailure(call: Call<KeyPointRestrictions>, t: Throwable) {
                MainScope().launch {
                    Toast.makeText(
                        context,
                        "Failed to get exercise response from API !!!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
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

    fun getPhaseSummary(): List<PhaseSummary> = phaseSummary

    fun pauseExercise() {
        takingRest = true
        manuallyPaused = true
    }

    fun resumeExercise() {
        takingRest = false
        manuallyPaused = false
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
            val instruction = getInstruction(firstInstruction)
            delay(firstDelay)
            asyncAudioPlayer.playText(instruction)
            delay(secondDelay)
            secondInstruction?.let {
                asyncAudioPlayer.playText(getInstruction(it))
            }
            if (shouldTakeRest and !manuallyPaused) takingRest = false
        }
    }

    fun getPersonDistance(person: Person): Float {
        return if (System.currentTimeMillis() >= lastTimeDistanceCalculated + distanceCalculationInterval) {
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
            distanceFromCamera = distance?.let { it / 12 } ?: 4f
            distanceFromCamera
        } else {
            distanceFromCamera
        }
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
        if (phaseIndex < rightCountPhases.size) {
            val phase = rightCountPhases[phaseIndex]
            val minConfidenceSatisfied = isMinConfidenceSatisfied(phase, person)
            if (rightCountPhases.isNotEmpty() && minConfidenceSatisfied && !takingRest) {
                val constraintSatisfied = isConstraintSatisfied(
                    person,
                    phase.constraints
                )
                if (setCounter == 0) {
                    trackMinMaxConstraints(person = person)
                }
                if (VisualizationUtils.isInsideBox(
                        person,
                        consideredIndices.toList(),
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
                            repetitionCount()
                        } else {
                            phaseIndex++
                            rightCountPhases[phaseIndex].phaseDialogue?.let {
                                playInstruction(firstDelay = 500L, firstInstruction = it)
                            }
                            downTimeCounter = 0
                        }
                    } else {
                        if (phaseIndex != 0) countDownAudio(downTimeCounter)
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
                exerciseInstruction(person)
            }
        }
    }

    open fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {}

    open fun exerciseInstruction(person: Person) {}

    private fun isMinConfidenceSatisfied(phase: Phase, person: Person): Boolean {
        val indices = mutableSetOf<Int>()
        var isSatisfied = true
        phase.constraints.forEach {
            indices.add(it.startPointIndex)
            indices.add(it.middlePointIndex)
            indices.add(it.endPointIndex)
        }
        for (index in 0 until person.keyPoints.size) {
            if (person.keyPoints[index].bodyPart.position in indices && person.keyPoints[index].score < VisualizationUtils.MIN_CONFIDENCE) {
                isSatisfied = false
                break
            }
        }
        return isSatisfied
    }

    private fun getInstruction(text: String): Instruction {
        var instruction = instructions.find {
            it.text.lowercase() == text.lowercase()
        }
        if (instruction == null) {
            instruction = asyncAudioPlayer.generateInstruction(text)
            instructions.add(instruction)
        }
        return instruction
    }

    private fun repetitionCount() {
        repetitionCounter++
        if (repetitionCounter >= maxRepCount) {
            repetitionCounter = 0
            setCounter++
            if (setCounter == maxSetCount) {
                asyncAudioPlayer.playText(getInstruction(AsyncAudioPlayer.FINISH))
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
            if (setCounter == 1) {
                setNewConstraints()
            }
        } else {
            val phase = rightCountPhases[0]
            if (phase.holdTime > 0) {
                val repetitionInstruction = getInstruction(repetitionCounter.toString())
                playInstruction(
                    firstDelay = 0L,
                    firstInstruction = repetitionCounter.toString(),
                    secondDelay = repetitionInstruction.player?.duration?.toLong() ?: 500L,
                    secondInstruction = if (playPauseCue) AsyncAudioPlayer.PAUSE else null,
                    shouldTakeRest = true
                )
            } else {
                playInstruction(
                    firstDelay = 0L,
                    firstInstruction = repetitionCounter.toString()
                )
            }
        }
    }

    private fun setNewConstraints() {
        trackIndex = 0
        //Log.d("setCount", "right count phases: $rightCountPhases \n \n ")
        rightCountPhases.forEach { phase ->
            phase.constraints.forEach { constraint ->
                val standardValues = constraint.getStandardConstraints()
                val standardConstraintGap = standardValues.standardMax - standardValues.standardMin
                val minMaxMedian = constraint.getMinMaxMedian()
                val refinedConstraintGap = minMaxMedian.max - minMaxMedian.max

                if (minMaxMedian.median < standardValues.standardMin || minMaxMedian.median > standardValues.standardMax) {
                    //Log.d("setCount", "Out of the standard constraints!")
                    if (refinedConstraintGap > standardConstraintGap) {
                        val newMin = minMaxMedian.median - (standardConstraintGap / 2)
                        val newMax = minMaxMedian.median + (standardConstraintGap / 2)
                        constraint.setRefinedConstraints(min = newMin, max = newMax)
                    } else {
                        val minimumGap = 15
                        if (refinedConstraintGap <= minimumGap) {
                            val newMin = minMaxMedian.median - (minimumGap / 2)
                            val newMax = minMaxMedian.median + (minimumGap / 2)
                            constraint.setRefinedConstraints(min = newMin, max = newMax)
                        } else {
                            val newMin = minMaxMedian.median - (refinedConstraintGap / 2)
                            val newMax = minMaxMedian.median + (refinedConstraintGap / 2)
                            constraint.setRefinedConstraints(min = newMin, max = newMax)
                        }
                    }

                } else {
                    //Log.d("setCount", "Satisfies the standard constraints!")
                    constraint.setStandardConstraints()
                }
                constraint.storedValues.clear()

                //Log.d("setCount", "phase:: ${phase.phaseNumber}")
                //Log.d("setCount", "median:: ${minMaxMedian.median}")
                //Log.d(
                //    "setCount",
                //    "Phase Details:: ${getIndexName(constraint.startPointIndex)}, " +
                //            getIndexName(constraint.middlePointIndex) + " ," +
                //            getIndexName(constraint.endPointIndex)
                //)

                //Log.d("setCount", "new minValue:: ${constraint.minValue}")
                //Log.d("setCount", "new maxValue:: ${constraint.maxValue}")

                restriction.add(
                    Restriction(
                        StartKeyPosition = getIndexName(constraint.startPointIndex),
                        MiddleKeyPosition = getIndexName(constraint.middlePointIndex),
                        EndKeyPosition = getIndexName(constraint.endPointIndex),
                        AverageMax = constraint.minValue,
                        AverageMin = constraint.maxValue
                    )
                )

                phaseSummary.add(
                    PhaseSummary(
                        PhaseNumber = phase.phaseNumber,
                        Restrictions = restriction.toMutableList()
                    )
                )
                restriction.clear()



                //Log.d("setCount", "Output:: ${phaseSummary}")
            }

        }

    }

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
                    val minValue = min(it.minValue, it.standardMin)
                    val maxValue = max(it.maxValue, it.standardMax)
                    if (angle < minValue.toFloat() || angle > maxValue.toFloat()) {
                        constraintSatisfied = false
                    }
                }
                ConstraintType.LINE -> {}
            }
        }
        return constraintSatisfied
    }

    private fun getCurrentPhase(person: Person, phases: List<Phase>): Int {
        val reversedPhases = phases.sortedBy { it.phaseNumber }.reversed()
        var index = phases.size - 2
        while (index >= 0) {
            if (isConstraintSatisfied(person, reversedPhases[index].constraints)) {
                return index
            }
            index--
        }
        return -1
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
                    consideredIndices.toList(),
                    canvasHeight,
                    canvasWidth
                )
            ) onEvent(CommonInstructionEvent.OutSideOfBox)
        }
        if (getPersonDistance(person) > MAX_DISTANCE_FROM_CAMERA) {
//            onEvent(CommonInstructionEvent.TooFarFromCamera)
        }
    }

    private fun countDownAudio(count: Int) {
        if (previousCountDown != count && count > 0) {
            previousCountDown = count
            if (count > 20) {
                asyncAudioPlayer.playText(getInstruction(AsyncAudioPlayer.BEEP))
            } else {
                asyncAudioPlayer.playText(getInstruction(count.toString()))
            }
        }
    }

    sealed class CommonInstructionEvent {
        object OutSideOfBox : CommonInstructionEvent()
        object HandIsNotStraight : CommonInstructionEvent()
        object LeftHandIsNotStraight : CommonInstructionEvent()
        object RightHandIsNotStraight : CommonInstructionEvent()
        object TooFarFromCamera : CommonInstructionEvent()
    }

    private fun trackMinMaxConstraints(person: Person) {
        rightCountPhases[trackIndex].constraints.forEach {
            when (it.type) {
                ConstraintType.ANGLE -> {
                    val angle = Utilities.angle(
                        startPoint = person.keyPoints[it.startPointIndex].toRealPoint(),
                        middlePoint = person.keyPoints[it.middlePointIndex].toRealPoint(),
                        endPoint = person.keyPoints[it.endPointIndex].toRealPoint(),
                        clockWise = it.clockWise
                    )
                    if (angle < it.minValue || angle > it.maxValue) {
                        if (phaseIndex != trackIndex) {
                            trackIndex = phaseIndex
                        }
                    } else {
                        it.storedValues.add(angle.toInt())
                    }
                }
                ConstraintType.LINE -> {}
            }
        }
    }
}
