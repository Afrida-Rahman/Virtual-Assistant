package org.mmh.virtual_assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.Point
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.speech.SpeechRecognizer
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.ExerciseTrackingPayload
import org.mmh.virtual_assistant.api.request.PhaseSummary
import org.mmh.virtual_assistant.api.request.QResponse
import org.mmh.virtual_assistant.api.response.ExerciseTrackingResponse
import org.mmh.virtual_assistant.core.*
import org.mmh.virtual_assistant.core.VisualizationUtils.isPointInsideRectangle
import org.mmh.virtual_assistant.domain.model.*
import org.mmh.virtual_assistant.exercise.home.GeneralExercise
import org.mmh.virtual_assistant.exercise.home.HomeExercise
import org.mmh.virtual_assistant.ml.MoveNet
import org.mmh.virtual_assistant.ml.PoseDetector
import org.mmh.virtual_assistant.voice.ContinuousRecognitionManager
import org.mmh.virtual_assistant.voice.RecognitionCallback
import org.mmh.virtual_assistant.voice.RecognitionStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


@Suppress("DEPRECATION")
class ExerciseActivity : AppCompatActivity(), RecognitionCallback {
    companion object {
        const val ExerciseId = "ExerciseId"
        const val TestId = "TestId"
        const val Name = "Name"
        const val ProtocolId = "ProtocolId"
        const val RepetitionLimit = "RepetitionLimit"
        const val SetLimit = "SetLimit"
        const val ImageUrl = "ImageUrl"
        const val TAG = "ExerciseActivityTag"
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480

        private const val ACTIVATION_KEYWORD = "Hello"
        private const val RECORD_AUDIO_REQUEST_CODE = 101
    }

    private val lock = Any()
    private var enableAskQues = false
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var backgroundHandler: Handler? = null
    private var previewSize: Size? = null
    private var backgroundThread: HandlerThread? = null
    private var cameraId: String = ""
    private var previewWidth = 0
    private var previewHeight = 0
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var poseDetector: PoseDetector? = null
    private var device = Device.GPU
    private var modelPos = 2
    private var imageReader: ImageReader? = null
    private val minConfidence = .2f
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null

    private lateinit var tvScore: TextView
    private lateinit var tvTime: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnModel: Spinner

    private lateinit var saveExerciseTrackingURL: String

    private lateinit var exercise: HomeExercise

    private var isFrontCamera = true
    private var showCongrats = false
    private lateinit var logInData: LogInData

    private var testId: String? = ""
    private var exerciseId: Int = 0
    private var protocolId: Int = 0
    private var qResponse = mutableListOf<QResponse>()

    private lateinit var countDisplay: TextView
    private lateinit var distanceDisplay: TextView
    private lateinit var wrongCountDisplay: TextView
    private lateinit var timeCountDisplay: TextView
    private lateinit var phaseDialogueDisplay: TextView
    private lateinit var maxHoldTimeDisplay: TextView
    private lateinit var exerciseProgressBar: ProgressBar
    private lateinit var gifButton: ImageButton
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button
    private lateinit var pauseIndicator: ImageView
    private lateinit var question: VisualQues
    private var quesAnsweredAt = System.currentTimeMillis()
    private var quesDelay = 3000L

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@ExerciseActivity.cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(camera)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
            openCamera()
        } else {
            Toast.makeText(
                this, "Camera permission is required to use this feature!", Toast.LENGTH_LONG
            ).show()
        }
    }

    private var imageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(imageReader: ImageReader) {
            // We need wait until we have some size from onPreviewSizeChosen
            if (previewWidth == 0 || previewHeight == 0) {
                return
            }

            val image = imageReader.acquireLatestImage() ?: return
            val nv21Buffer =
                ImageUtils.yuv420ThreePlanesToNV21(image.planes, previewWidth, previewHeight)
            val imageBitmap = ImageUtils.getBitmap(nv21Buffer!!, previewWidth, previewHeight)

            // Create rotated version for portrait display
            val rotateMatrix = Matrix()
            if (isFrontCamera) {
                rotateMatrix.postRotate(-90.0f)
            } else {
                rotateMatrix.postRotate(90.0f)
            }
            val rotatedBitmap = Bitmap.createBitmap(
                imageBitmap!!, 0, 0, previewWidth, previewHeight, rotateMatrix, true
            )
            image.close()

            processImage(rotatedBitmap)
        }
    }

    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?, view: View?, position: Int, id: Long
        ) {
            changeModel(position)
        }
    }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private val recognitionManager: ContinuousRecognitionManager by lazy {
        ContinuousRecognitionManager(this, activationKeyword = ACTIVATION_KEYWORD, callback = this)
    }
    lateinit var progressBar: ProgressBar
    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        val screenDimensions = getScreenSizeInlcudingTopBottomBar(this)

//        PREVIEW_HEIGHT = screenDimensions[0]
//        PREVIEW_WIDTH = screenDimensions[1]

        testId = intent.getStringExtra(TestId)
        exerciseId = intent.getIntExtra(ExerciseId, 122)
        protocolId = intent.getIntExtra(ProtocolId, 1)
        val exerciseName = intent.getStringExtra(Name)
        val repetitionLimit = intent.getIntExtra(RepetitionLimit, 5)
        val setLimit = intent.getIntExtra(SetLimit, 1)
        val imageUrl = intent.getStringExtra(ImageUrl)
        logInData = loadLogInData()

        val existingExercise = Exercises.get(this, exerciseId)
        exercise = existingExercise ?: GeneralExercise(
            context = this, exerciseId = exerciseId, active = true
        )
        exercise.initializeConstraint(logInData.tenant)
        exercise.setExercise(
            exerciseName = exerciseName ?: "",
            exerciseInstruction = "",
            exerciseImageUrls = listOf(),
            exerciseVideoUrls = "",
            repetitionLimit = repetitionLimit,
            setLimit = setLimit,
            protoId = protocolId,
        )

        countDisplay = findViewById(R.id.right_count)
        distanceDisplay = findViewById(R.id.distance)
        wrongCountDisplay = findViewById(R.id.wrong_count)
        timeCountDisplay = findViewById(R.id.time_count_display)
        phaseDialogueDisplay = findViewById(R.id.phase_dialogue)
        maxHoldTimeDisplay = findViewById(R.id.max_hold_time_display)
        exerciseProgressBar = findViewById(R.id.exercise_progress)
        gifButton = findViewById(R.id.btn_gif_display)
        pauseButton = findViewById(R.id.btn_pause)
        resumeButton = findViewById(R.id.btn_resume)
        pauseIndicator = findViewById(R.id.pause_indicator)

        exerciseProgressBar.max = exercise.maxSetCount * exercise.maxRepCount

        maxHoldTimeDisplay.text = getString(R.string.max_time_hold).format(0)

        countDisplay.text = getString(R.string.right_count_text).format(
            exercise.getRepetitionCount(), exercise.getSetCount()
        )
        distanceDisplay.text = getString(R.string.distance_text).format(0f)
        wrongCountDisplay.text = getString(R.string.wrong_text).format(0)

        phaseDialogueDisplay.visibility = View.GONE

        findViewById<TextView>(R.id.exercise_name).text = exerciseName

        findViewById<Button>(R.id.btn_done).setOnClickListener {
//            saveExerciseData(
//                Tenant = logInData.tenant,
//                PatientId = logInData.patientId,
//                TestId = testId!!,
//                ExerciseId = exerciseId,
//                ProtocolId = protocolId,
//                ExerciseDate = Utilities.currentDate(),
//                NoOfReps = exercise.getRepetitionCount(),
//                NoOfSets = exercise.getSetCount(),
//                NoOfWrongCount = exercise.getWrongCount(),
//                AssignSets = exercise.maxSetCount,
//                AssignReps = exercise.maxRepCount,
//                TotalTime = 0,
//                Phases = exercise.getPhaseSummary(),
//                Responses = listOf()
//            )
//            askQuestions(this)
            if(!exercise.isAsyncAudioPlayerInitialized()){
                Toast.makeText(applicationContext,"Please wait for initialization.", Toast.LENGTH_SHORT).show()
            } else {
                for (instruction in exercise.instructions){
                    instruction.player?.stop()
                }
                pauseButton.visibility = View.GONE
                askVizQuestions(10001000)
            }

        }

        pauseButton.setOnClickListener {
            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.VISIBLE
            pauseIndicator.visibility = View.VISIBLE
            exercise.pauseExercise()
        }

        resumeButton.setOnClickListener {
            resumeButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            pauseIndicator.visibility = View.GONE
            exercise.resumeExercise()
        }

        findViewById<ImageButton>(R.id.camera_switch_button).setOnClickListener {
            isFrontCamera = !isFrontCamera
            closeCamera()
            openCamera()
        }

        if (imageUrl != null) {
            findViewById<ImageButton>(R.id.btn_gif_display).setOnClickListener {
                showExerciseInformation(this, imageUrl)
            }
        } else {
            gifButton.visibility = View.GONE
        }


        createPoseEstimator()

        tvScore = findViewById(R.id.tvScore)
        tvTime = findViewById(R.id.tvTime)
        spnModel = findViewById(R.id.spnModel)
        spnDevice = findViewById(R.id.spnDevice)
        surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView.holder

        initSpinner()
        requestPermission()
        closeCamera()
        openCamera()

        // Voice Recognition
        progressBar = findViewById(R.id.progressBar)
        textView = findViewById(R.id.textView)

        progressBar.visibility = View.INVISIBLE
        progressBar.max = 10

        recognitionManager.createRecognizer()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        }
    }

    fun getScreenSizeInlcudingTopBottomBar(context: Context): IntArray {
        val screenDimensions = IntArray(2) // width[0], height[1]
        val x: Int
        val y: Int
        val orientation = context.resources.configuration.orientation
        val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val screenSize = Point()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(screenSize)
                x = screenSize.x
                y = screenSize.y
            } else {
                display.getSize(screenSize)
                x = screenSize.x
                y = screenSize.y
            }
        } else {
            x = display.width
            y = display.height
        }
        screenDimensions[0] =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) x else y // width
        screenDimensions[1] =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) y else x // height
        return screenDimensions
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecognition()
        }
        startBackgroundThread()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        stopRecognition()
        super.onPause()
    }

    override fun onDestroy() {
        recognitionManager.destroyRecognizer()
        super.onDestroy()
        poseDetector?.close()
    }

    private fun changeModel(position: Int) {
        modelPos = position
        createPoseEstimator()
    }

    private fun changeDevice(position: Int) {
        device = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        createPoseEstimator()
    }

    private fun createPoseEstimator() {
        closeCamera()
        stopBackgroundThread()
        poseDetector?.close()
        poseDetector = null
        poseDetector = MoveNet.create(this, device)

        openCamera()
        startBackgroundThread()
    }

    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this, R.array.tfe_pe_models_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            this, R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun openCamera() {
        // check if permission is granted or not.
        if (checkPermission(
                Manifest.permission.CAMERA, Process.myPid(), Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setUpCameraOutputs()
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }

    private fun setUpCameraOutputs() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)

                exercise.setFocalLength(characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS))

                if (isFrontCamera) {
                    if (cameraDirection != null && cameraDirection != CameraCharacteristics.LENS_FACING_FRONT) {
                        continue
                    }
                } else {
                    if (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue
                    }
                }

                previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)

                imageReader = ImageReader.newInstance(
                    PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, /*maxImages*/ 2
                )

                previewHeight = previewSize!!.height
                previewWidth = previewSize!!.width

                this.cameraId = cameraId

                // We've found a viable camera and finished setting up member variables,
                // so we don't need to iterate through other available cameras.
                return
            }
        } catch (e: CameraAccessException) {
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("imageAvailableListener").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // do nothing
        }
    }

    private fun createCameraPreviewSession() {
        try {
            // We capture images from preview in YUV format.
            imageReader = ImageReader.newInstance(
                previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2
            )
            imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

            // This is the surface we need to record images for processing.
            val recordingSurface = imageReader!!.surface

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder!!.addTarget(recordingSurface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice!!.createCaptureSession(
                listOf(recordingSurface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (cameraDevice == null) return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder!!.build()
                            captureSession!!.setRepeatingRequest(
                                previewRequest!!, null, null
                            )
                        } catch (e: CameraAccessException) {
                            //Log.e(TAG, e.toString())
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
//                        Toast.makeText(this@ExerciseActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            //Log.e(TAG, "Error creating camera preview session.", e)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun processImage(bitmap: Bitmap) {
        var score = 0f
        var outputBitmap = bitmap
        var cordRightWrist: PointF? = null
        var cordLeftWrist: PointF? = null
        var vizOutput: VisualOutput? = null

        if ((exercise.getSetCount() >= exercise.maxSetCount) && !showCongrats) {
            showCongrats = true
            MainScope().launch {
                congratsPatient(context = this@ExerciseActivity)
            }
        }
        synchronized(lock) {
            poseDetector?.estimateSinglePose(bitmap)?.let { person ->
                score = person.score
                if (score > minConfidence) {
                    val height = bitmap.height
                    val width = bitmap.width
                    if (!showCongrats && !enableAskQues) {
                        exercise.rightExerciseCount(person, height, width)
                        exercise.wrongExerciseCount(person, height, width)
                    }
                    val phase = exercise.getPhase()
                    MainScope().launch {
                        if (enableAskQues) {
                            phaseDialogueDisplay.visibility = View.VISIBLE
                            gifButton.visibility = View.GONE
                            phaseDialogueDisplay.text = question.questionMessage
                            val width = Resources.getSystem().displayMetrics.widthPixels
                            val textSize = 0.0394f*width - 0.5f*question.questionMessage.length
                            phaseDialogueDisplay.setTextSize(TypedValue.COMPLEX_UNIT_DIP,textSize)
                            phaseDialogueDisplay.maxLines = 1
                            findViewById<Button>(R.id.btn_done).visibility = View.GONE
                            exercise.getPersonDistance(person).let {
                                distanceDisplay.text = getString(R.string.distance_text).format(it)
                            }
                        } else {
                            countDisplay.text = getString(R.string.right_count_text).format(
                                exercise.getRepetitionCount(), exercise.getSetCount()
                            )
                            exercise.getPersonDistance(person).let {
                                distanceDisplay.text = getString(R.string.distance_text).format(it)
                                if (it <= 5f) {
                                    phaseDialogueDisplay.textSize = 30f
                                } else if (5f < it && it <= 10f) {
                                    phaseDialogueDisplay.textSize = 50f
                                } else {
                                    phaseDialogueDisplay.textSize = 70f
                                }
                            }

//                    wrongCountDisplay.text =
//                        getString(R.string.wrong_text).format(exercise.getWrongCount())
                            phase?.let {
                                val timeToDisplay = exercise.getHoldTimeLimitCount()
                                it.phaseDialogue?.let { dialogue ->
                                    if (dialogue.isNotEmpty()) {
                                        phaseDialogueDisplay.visibility = View.VISIBLE
                                        phaseDialogueDisplay.text =
                                            getString(R.string.phase_dialogue).format(dialogue)
                                    } else {
                                        phaseDialogueDisplay.visibility = View.GONE
                                    }
                                }
                                if (timeToDisplay > 0) {
                                    timeCountDisplay.visibility = View.VISIBLE
                                    timeCountDisplay.text =
                                        getString(R.string.time_count_text).format(timeToDisplay)
                                } else {
                                    timeCountDisplay.visibility = View.GONE
                                    timeCountDisplay.text =
                                        getString(R.string.time_count_text).format(0)
                                }
                            }
                            maxHoldTimeDisplay.text =
                                getString(R.string.max_time_hold).format(exercise.getMaxHoldTime())
                            exerciseProgressBar.progress =
                                exercise.getSetCount() * exercise.maxRepCount + exercise.getRepetitionCount()
                        }
                    }

                    if (enableAskQues) {
                        for (keyPoint in person.keyPoints) {
                            if (keyPoint.bodyPart == BodyPart.RIGHT_WRIST && keyPoint.score > minConfidence) {
                                cordRightWrist = keyPoint.coordinate
                            } else if (keyPoint.bodyPart == BodyPart.LEFT_WRIST && keyPoint.score > minConfidence) {
                                cordLeftWrist = keyPoint.coordinate
                            }
                        }
                    }

                    var drawButton = false
                    if (System.currentTimeMillis() - quesAnsweredAt > quesDelay && enableAskQues) {
                        drawButton = true
                    }
                    vizOutput = VisualizationUtils.drawBodyKeyPoints(
                        input = outputBitmap,
                        person = person,
                        phase = phase,
                        isFrontCamera = isFrontCamera,
                        consideredIndices = exercise.consideredIndices.toList(),
                        enableAskQues = drawButton
                    )
                    outputBitmap = vizOutput!!.bitmap
                }
            }
        }

        val canvas: Canvas = surfaceHolder.lockCanvas()
        if (isFrontCamera) {
            canvas.scale(-1f, 1f, canvas.width.toFloat() / 2, canvas.height.toFloat() / 2)
        }

        val screenWidth: Int
        val screenHeight: Int
        val left: Int
        val top: Int

        if (canvas.height > canvas.width) {
            val ratio = outputBitmap.height.toFloat() / outputBitmap.width
            screenWidth = canvas.width
            left = 0
            screenHeight = (canvas.width * ratio).toInt()
            top = (canvas.height - screenHeight) / 2
        } else {
            val ratio = outputBitmap.width.toFloat() / outputBitmap.height
            screenHeight = canvas.height
            top = 0
            screenWidth = (canvas.height * ratio).toInt()
            left = (canvas.width - screenWidth) / 2
        }
        val right: Int = left + screenWidth
        val bottom: Int = top + screenHeight

        canvas.drawBitmap(
            outputBitmap,
            Rect(0, 0, outputBitmap.width, outputBitmap.height),
            Rect(left, top, right, bottom),
            Paint()
        )
        surfaceHolder.unlockCanvasAndPost(canvas)

        // Process visual answer
        if (enableAskQues && (cordRightWrist != null && cordLeftWrist != null)) {
            if (System.currentTimeMillis() - quesAnsweredAt > quesDelay) {
                extractVisualAnswer(vizOutput!!.button.negRectF, vizOutput!!.button.posRectF, cordRightWrist!!, cordLeftWrist!!)
            }
        }

        tvScore.text = getString(R.string.tfe_pe_tv_score).format(score)
        poseDetector?.lastInferenceTimeNanos()?.let {
            tvTime.text = getString(R.string.tfe_pe_tv_time).format(it * 1.0f / 1_000_000)
        }
    }

    private fun congratsPatient(context: Context) {
        val alert = VisualizationUtils.getAlertDialogue(context = context,
            message = "Congratulations! You have successfully completed the exercise. Please be prepared for the next one.",
            positiveButtonText = "Ok",
            positiveButtonAction = {
                for (instruction in exercise.instructions){
                    if (instruction.text.lowercase() == AsyncAudioPlayer.CONGRATS){
                        instruction.player?.stop()
                    }
                }
//                askQuestions(context)
                askVizQuestions(10001000)
            },
            negativeButtonText = null,
            negativeButtonAction = {}).show()

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                alert.dismiss()
                askVizQuestions(10001000)
                timer.cancel()
            }
        }, 9000)
    }

    private fun askQuestions(context: Context) {
        val askForPain = VisualizationUtils.getAlertDialogue(context = context,
            message = "Do you find this exercise to be too painful?",
            positiveButtonText = "Yes",
            positiveButtonAction = {
                qResponse.add(
                    QResponse(
                        QuestionId = 10001002, AnswerId = 61894, AnswerValue = "Yes"
                    )
                )
                saveExerciseData(
                    Tenant = logInData.tenant,
                    PatientId = logInData.patientId,
                    TestId = testId!!,
                    ExerciseId = exerciseId,
                    ProtocolId = protocolId,
                    ExerciseDate = Utilities.currentDate(),
                    NoOfReps = exercise.getRepetitionCount(),
                    NoOfSets = exercise.getSetCount(),
                    NoOfWrongCount = exercise.getWrongCount(),
                    AssignSets = exercise.maxSetCount,
                    AssignReps = exercise.maxRepCount,
                    TotalTime = 0,
                    Phases = exercise.getPhaseSummary(),
                    Responses = qResponse
                )
                finish()
            },
            negativeButtonText = "No",
            negativeButtonAction = {
                qResponse.add(
                    QResponse(
                        QuestionId = 10001002, AnswerId = 61893, AnswerValue = "No"
                    )
                )
                saveExerciseData(
                    Tenant = logInData.tenant,
                    PatientId = logInData.patientId,
                    TestId = testId!!,
                    ExerciseId = exerciseId,
                    ProtocolId = protocolId,
                    ExerciseDate = Utilities.currentDate(),
                    NoOfReps = exercise.getRepetitionCount(),
                    NoOfSets = exercise.getSetCount(),
                    NoOfWrongCount = exercise.getWrongCount(),
                    AssignSets = exercise.maxSetCount,
                    AssignReps = exercise.maxRepCount,
                    TotalTime = 0,
                    Phases = exercise.getPhaseSummary(),
                    Responses = qResponse
                )
                finish()
            })
        val askForEasiness = VisualizationUtils.getAlertDialogue(context = context,
            message = "Was this exercise too easy?",
            positiveButtonText = "Yes",
            positiveButtonAction = {
                qResponse.add(
                    QResponse(
                        QuestionId = 10001001, AnswerId = 61892, AnswerValue = "Yes"
                    )
                )
                saveExerciseData(
                    Tenant = logInData.tenant,
                    PatientId = logInData.patientId,
                    TestId = testId!!,
                    ExerciseId = exerciseId,
                    ProtocolId = protocolId,
                    ExerciseDate = Utilities.currentDate(),
                    NoOfReps = exercise.getRepetitionCount(),
                    NoOfSets = exercise.getSetCount(),
                    NoOfWrongCount = exercise.getWrongCount(),
                    AssignSets = exercise.maxSetCount,
                    AssignReps = exercise.maxRepCount,
                    TotalTime = 0,
                    Phases = exercise.getPhaseSummary(),
                    Responses = qResponse
                )
                finish()
            },
            negativeButtonText = "No",
            negativeButtonAction = {
                askForPain.show()
                qResponse.add(
                    QResponse(
                        QuestionId = 10001001, AnswerId = 61891, AnswerValue = "No"
                    )
                )
            })
        VisualizationUtils.getAlertDialogue(context = context,
            message = "Did you find this exercise to be too difficult?",
            positiveButtonText = "Yes",
            positiveButtonAction = {
                qResponse.add(
                    QResponse(
                        QuestionId = 10001000, AnswerId = 61890, AnswerValue = "Yes"
                    )
                )
                saveExerciseData(
                    Tenant = logInData.tenant,
                    PatientId = logInData.patientId,
                    TestId = testId!!,
                    ExerciseId = exerciseId,
                    ProtocolId = protocolId,
                    ExerciseDate = Utilities.currentDate(),
                    NoOfReps = exercise.getRepetitionCount(),
                    NoOfSets = exercise.getSetCount(),
                    NoOfWrongCount = exercise.getWrongCount(),
                    AssignSets = exercise.maxSetCount,
                    AssignReps = exercise.maxRepCount,
                    TotalTime = 0,
                    Phases = exercise.getPhaseSummary(),
                    Responses = qResponse
                )
                finish()
            },
            negativeButtonText = "No",
            negativeButtonAction = {
                askForEasiness.show()
                qResponse.add(
                    QResponse(
                        QuestionId = 10001000, AnswerId = 61889, AnswerValue = "No"
                    )
                )
            }).show()
    }

    private fun saveExerciseData(
        Tenant: String,
        PatientId: String,
        TestId: String,
        ExerciseId: Int,
        ProtocolId: Int,
        ExerciseDate: String,
        AssignSets: Int = 0,
        AssignReps: Int = 0,
        NoOfReps: Int,
        NoOfSets: Int,
        NoOfWrongCount: Int,
        TotalTime: Int = 0,
        Phases: List<PhaseSummary>,
        Responses: List<QResponse>
    ) {
        val logInData = loadLogInData()
        saveExerciseTrackingURL = Utilities.getUrl(logInData.tenant).saveExerciseTrackingURL

        val service = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
            .baseUrl(saveExerciseTrackingURL).build().create(IExerciseService::class.java)

        val requestPayload = ExerciseTrackingPayload(
            Tenant = Tenant,
            PatientId = PatientId,
            TestId = TestId,
            ExerciseId = ExerciseId,
            ProtocolId = ProtocolId,
            AssignSets = AssignSets,
            AssignReps = AssignReps,
            ExerciseDate = ExerciseDate,
            NoOfReps = NoOfReps,
            NoOfSets = NoOfSets,
            NoOfWrongCount = NoOfWrongCount,
            TotalTime = TotalTime,
            Phases = Phases,
            Responses = Responses
        )
//        Log.d(
//            "setCount",
//            "\n \n Tenant = $Tenant \n PatientId = $PatientId \n TestId = $TestId \n ExerciseId = $ExerciseId \n " + "ProtocolId = $ProtocolId \n AssignSets = $AssignSets \n AssignReps = $AssignReps \n ExerciseDate = $ExerciseDate \n" + "NoOfReps = $NoOfReps \nNoOfSets = $NoOfSets \n NoOfWrongCount = $NoOfWrongCount \n TotalTime = $TotalTime \n " + "Phases = $Phases \n,Responses = $Responses"
//        )
        val response = service.saveExerciseData(requestPayload)
        response.enqueue(object : Callback<ExerciseTrackingResponse> {
            override fun onResponse(
                call: Call<ExerciseTrackingResponse>, response: Response<ExerciseTrackingResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.Successful) {
                        Toast.makeText(
                            this@ExerciseActivity, responseBody.Message, Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ExerciseActivity,
                            "Could not save exercise data!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@ExerciseActivity,
                        "Failed to save and got empty response! ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ExerciseTrackingResponse>, t: Throwable) {
                Toast.makeText(
                    this@ExerciseActivity, "Failed to save exercise data !!!", Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun loadLogInData(): LogInData {
        val preferences = getSharedPreferences(
            SignInActivity.LOGIN_PREFERENCE, SignInActivity.PREFERENCE_MODE
        )
        return LogInData(
            firstName = preferences.getString(SignInActivity.FIRST_NAME, "") ?: "",
            lastName = preferences.getString(SignInActivity.LAST_NAME, "") ?: "",
            patientId = preferences.getString(SignInActivity.PATIENT_ID, "") ?: "",
            tenant = preferences.getString(SignInActivity.TENANT, "") ?: ""
        )
    }

    private fun showExerciseInformation(context: Context, url: String) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.image_popup_modal, null, false)
        val alertDialog = AlertDialog.Builder(context).setView(dialogView)
        val imageContainer: ImageView = dialogView.findViewById(R.id.gif_container)
        Glide.with(context).load(url).diskCacheStrategy(DiskCacheStrategy.DATA)
            .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade(500)).override(600)
            .into(imageContainer)
        alertDialog.setPositiveButton("Okay") { _, _ ->
        }
        alertDialog.show()
    }

    private fun askVizQuestions(quesId: Int) {
        question = VisualQues.fromQuesId(quesId)
        exercise.playInstruction(1500, question.questionMessage)
        enableAskQues = true
    }

    private fun extractVisualAnswer(noRectF: RectF, yesRectF: RectF, cordRightWrist: PointF, cordLeftWrist: PointF) {
        var negAnswer: Boolean
        var posAnswer: Boolean
        if(isFrontCamera){
            negAnswer = isPointInsideRectangle(noRectF, cordRightWrist)
            posAnswer = isPointInsideRectangle(yesRectF, cordLeftWrist)
        } else {
            negAnswer = isPointInsideRectangle(noRectF, cordLeftWrist)
            posAnswer = isPointInsideRectangle(yesRectF, cordRightWrist)
        }
        if (negAnswer && posAnswer){
            quesAnsweredAt = System.currentTimeMillis()
            Toast.makeText(applicationContext, "Please select one answer.", Toast.LENGTH_SHORT).show()
        } else if ((negAnswer && !isFrontCamera) || (posAnswer && isFrontCamera)) { // Answer is negative
            quesAnsweredAt = System.currentTimeMillis()
            enableAskQues = false
            when (question) {
                VisualQues.WAS_THIS_EXERCISE_TOO_EASY -> {
                    processAnswer(
                        question.quesId,
                        question.negAnsId,
                        "No",
                        VisualQues.DO_YOU_FIND_THIS_EXERCISE_TO_BE_TOO_PAINFUL.quesId
                    )
                }
                VisualQues.DID_YOU_FIND_THIS_EXERCISE_TO_BE_TOO_DIFFICULT -> {
                    processAnswer(
                        question.quesId,
                        question.negAnsId,
                        "No",
                        VisualQues.WAS_THIS_EXERCISE_TOO_EASY.quesId
                    )
                }
                VisualQues.DO_YOU_FIND_THIS_EXERCISE_TO_BE_TOO_PAINFUL -> {
                    processAnswer(question.quesId, question.negAnsId, "No")
                }
            }
        } else if ((posAnswer && !isFrontCamera) || (negAnswer && isFrontCamera)) { // Answer is positive
            quesAnsweredAt = System.currentTimeMillis()
            enableAskQues = false
            processAnswer(question.quesId, question.posAnsId, "Yes")
        }
    }

    private fun processAnswer(QuestionId: Int, AnswerId: Long, AnswerValue: String, nextQuesId: Int = 0) {
        qResponse.add(
            QResponse(
                QuestionId = QuestionId, AnswerId = AnswerId, AnswerValue = AnswerValue
            )
        )
        if(nextQuesId > 0){
            askVizQuestions(nextQuesId)
        } else {
            saveExerciseData(
                Tenant = logInData.tenant,
                PatientId = logInData.patientId,
                TestId = testId!!,
                ExerciseId = exerciseId,
                ProtocolId = protocolId,
                ExerciseDate = Utilities.currentDate(),
                NoOfReps = exercise.getRepetitionCount(),
                NoOfSets = exercise.getSetCount(),
                NoOfWrongCount = exercise.getWrongCount(),
                AssignSets = exercise.maxSetCount,
                AssignReps = exercise.maxRepCount,
                TotalTime = 0,
                Phases = exercise.getPhaseSummary(),
                Responses = qResponse
            )
            finish()
        }

    }


    // Voice Command Recognition
    private fun startRecognition() {
        progressBar.isIndeterminate = false
        progressBar.visibility = View.VISIBLE
        recognitionManager.startRecognition()
    }

    private fun stopRecognition() {
        progressBar.isIndeterminate = true
        progressBar.visibility = View.INVISIBLE
        recognitionManager.stopRecognition()
    }

    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
        SpeechRecognizer.ERROR_SERVER -> "Error from server"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Didn't understand, please try again."
    }

    override fun onBeginningOfSpeech() {
        Log.i("Recognition","onBeginningOfSpeech")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.i("Recognition", "onBufferReceived: $buffer")
    }

    override fun onEndOfSpeech() {
        Log.i("Recognition","onEndOfSpeech")
    }

    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        Log.i("Recognition","onError: $errorMessage")
        textView.text = errorMessage
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.i("Recognition","onEvent")
    }

    override fun onReadyForSpeech(params: Bundle) {
        Log.i("Recognition","onReadyForSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        progressBar.progress = rmsdB.toInt()
    }

    override fun onPrepared(status: RecognitionStatus) {
        when (status) {
            RecognitionStatus.SUCCESS -> {
                Log.i("Recognition","onPrepared: Success")
                textView.text = "Recognition ready"
            }
            RecognitionStatus.UNAVAILABLE -> {
                Log.i("Recognition", "onPrepared: Failure or unavailable")
                AlertDialog.Builder(this)
                    .setTitle("Speech Recognizer unavailable")
                    .setMessage("Your device does not support Speech Recognition. Sorry!")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    override fun onKeywordDetected() {
        Log.i("Recognition","keyword detected !!!")
        textView.text = "Keyword detected"
    }

    override fun onPartialResults(results: List<String>) {}

    override fun onResults(results: List<String>, scores: FloatArray?) {
        val text = results.joinToString(separator = "\n")
        Log.i("Recognition","onResults : $text")
        results.firstOrNull { it.contains(other = "Cannot perform exercise", ignoreCase = true) }
            ?.let {
                recognitionManager.destroyRecognizer()
                findViewById<Button>(R.id.btn_done).performClick()
            }
        textView.text = text
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecognition()
                }
            }
        }
    }
}
