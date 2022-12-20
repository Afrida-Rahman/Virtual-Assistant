package org.mmh.virtual_assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.ExerciseTrackingPayload
import org.mmh.virtual_assistant.api.response.ExerciseTrackingResponse
import org.mmh.virtual_assistant.core.Exercises
import org.mmh.virtual_assistant.core.ImageUtils
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.Device
import org.mmh.virtual_assistant.domain.model.LogInData
import org.mmh.virtual_assistant.exercise.home.HomeExercise
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ExerciseActivity : AppCompatActivity() {
    companion object {
        const val ExerciseId = "ExerciseId"
        const val TestId = "TestId"
        const val Name = "Name"
        const val ProtocolId = "ProtocolId"
        const val RepetitionLimit = "RepetitionLimit"
        const val SetLimit = "SetLimit"
        const val ImageUrl = "ImageUrl"
        const val TAG = "ExerciseActivityTag"
        private var PREVIEW_WIDTH = 640
        private var PREVIEW_HEIGHT = 480
    }

    private val lock = Any()
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

    private lateinit var countDisplay: TextView
    private lateinit var distanceDisplay: TextView
    private lateinit var wrongCountDisplay: TextView
    private lateinit var timeCountDisplay: TextView
    private lateinit var phaseDialogueDisplay: TextView
    private lateinit var maxHoldTimeDisplay: TextView
    private lateinit var exerciseProgressBar: ProgressBar
    private lateinit var gifButton: ImageButton

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

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to use this feature!",
                    Toast.LENGTH_LONG
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
                imageBitmap!!, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                rotateMatrix, true
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
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val screenDimensions = getScreenSizeInlcudingTopBottomBar(this)

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

        exercise = Exercises.get(this, exerciseId)
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

        exerciseProgressBar.max = exercise.maxSetCount * exercise.maxRepCount

        maxHoldTimeDisplay.text =
            getString(R.string.max_time_hold).format(0)

        countDisplay.text = getString(R.string.right_count_text).format(
            exercise.getRepetitionCount(),
            exercise.getSetCount()
        )
        distanceDisplay.text = getString(R.string.distance_text).format(0f)
        wrongCountDisplay.text =
            getString(R.string.wrong_text).format(0)

        phaseDialogueDisplay.visibility = View.GONE

        findViewById<TextView>(R.id.exercise_name).text = exerciseName

        findViewById<Button>(R.id.btn_done).setOnClickListener {
            saveExerciseData(
                ExerciseId = exerciseId,
                TestId = testId!!,
                ProtocolId = protocolId,
                PatientId = logInData.patientId,
                ExerciseDate = Utilities.currentDate(),
                NoOfReps = exercise.getRepetitionCount(),
                NoOfSets = exercise.getSetCount(),
                NoOfWrongCount = exercise.getWrongCount(),
                Tenant = logInData.tenant
            )
            askQuestions(this)
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
    }

    ///  Get Screen width & height including top & bottom navigation bar
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
        screenDimensions[0] = if (orientation == Configuration.ORIENTATION_PORTRAIT) x else y // width
        screenDimensions[1] = if (orientation == Configuration.ORIENTATION_PORTRAIT) y else x // height
        return screenDimensions
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onDestroy() {
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
            this,
            R.array.tfe_pe_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
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
                Manifest.permission.CAMERA,
                Process.myPid(),
                Process.myUid()
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
                    PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    ImageFormat.YUV_420_888, /*maxImages*/ 2
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
                listOf(recordingSurface),
                object : CameraCaptureSession.StateCallback() {
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
                                previewRequest!!,
                                null, null
                            )
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
//                        Toast.makeText(this@ExerciseActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error creating camera preview session.", e)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun processImage(bitmap: Bitmap) {
        var score = 0f
        var outputBitmap = bitmap

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
                    if (!showCongrats) {
                        exercise.rightExerciseCount(person, height, width)
                        exercise.wrongExerciseCount(person, height, width)
                    }
                    val phase = exercise.getPhase()
                    MainScope().launch {
                        countDisplay.text = getString(R.string.right_count_text).format(
                            exercise.getRepetitionCount(),
                            exercise.getSetCount()
                        )
                        exercise.getPersonDistance(person)?.let {
                            distanceDisplay.text = getString(R.string.distance_text).format(it)
                            if (it <= 5f) {
                                phaseDialogueDisplay.textSize = 30f
                            } else if (5f < it && it <= 10f) {
                                phaseDialogueDisplay.textSize = 50f
                            } else {
                                phaseDialogueDisplay.textSize = 70f
                            }
                        }

                        wrongCountDisplay.text =
                            getString(R.string.wrong_text).format(exercise.getWrongCount())
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

//                    outputBitmap = Bitmap.createScaledBitmap(
//                        outputBitmap, 1080, 1080, false
//                    )
                    outputBitmap = VisualizationUtils.drawBodyKeyPoints(
                        input = outputBitmap,
                        person = person,
                        phase = phase,
                        isFrontCamera = isFrontCamera
                    )
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
            outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
            Rect(left, top, right, bottom), Paint()
        )
        surfaceHolder.unlockCanvasAndPost(canvas)
        tvScore.text = getString(R.string.tfe_pe_tv_score).format(score)
        poseDetector?.lastInferenceTimeNanos()?.let {
            tvTime.text =
                getString(R.string.tfe_pe_tv_time).format(it * 1.0f / 1_000_000)
        }
    }

    private fun congratsPatient(context: Context) {
        saveExerciseData(
            ExerciseId = exerciseId,
            TestId = testId!!,
            ProtocolId = protocolId,
            PatientId = logInData.patientId,
            ExerciseDate = Utilities.currentDate(),
            NoOfReps = exercise.getRepetitionCount(),
            NoOfSets = exercise.getSetCount(),
            NoOfWrongCount = exercise.getWrongCount(),
            Tenant = logInData.tenant
        )

        Log.d("getExercise", "$ExerciseId, $testId, $protocolId")
        VisualizationUtils.getAlertDialogue(
            context = context,
            message = "Congratulations! You have successfully completed the exercise. Please be prepared for the next one.",
            positiveButtonText = "Ok",
            positiveButtonAction = {
                askQuestions(context)
            },
            negativeButtonText = null,
            negativeButtonAction = {}
        ).show()
    }

    private fun askQuestions(context: Context) {
        val askForTracking = VisualizationUtils.getAlertDialogue(
            context = context,
            message = "Do you want to track your pain with EMMA?",
            positiveButtonText = "Yes",
            positiveButtonAction = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://emma.mypainlog.ai/#/new-screenings?patientid=${logInData.patientId}&type=refer&refertype=painlog&autologin=true")
                )
                startActivity(intent)
                finish()
            },
            negativeButtonText = "No",
            negativeButtonAction = {
                finish()
            }
        )
        VisualizationUtils.getAlertDialogue(
            context = context,
            message = "Do you feel any pain while performing this exercise?",
            positiveButtonText = "Yes",
            positiveButtonAction = {
                askForTracking.show()
            },
            negativeButtonText = "No",
            negativeButtonAction = {
                finish()
            }
        ).show()
    }

    private fun saveExerciseData(
        ExerciseId: Int,
        TestId: String,
        ProtocolId: Int,
        PatientId: String,
        ExerciseDate: String,
        NoOfReps: Int,
        NoOfSets: Int,
        NoOfWrongCount: Int,
        Tenant: String
    ) {
        val logInData = loadLogInData()
        saveExerciseTrackingURL = Utilities.getUrl(logInData.tenant).saveExerciseTrackingURL

        val service = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(saveExerciseTrackingURL)
            .build()
            .create(IExerciseService::class.java)

        val requestPayload = ExerciseTrackingPayload(
            ExerciseId = ExerciseId,
            TestId = TestId,
            ProtocolId = ProtocolId,
            PatientId = PatientId,
            ExerciseDate = ExerciseDate,
            NoOfReps = NoOfReps,
            NoOfSets = NoOfSets,
            NoOfWrongCount = NoOfWrongCount,
            Tenant = Tenant
        )
        val response = service.saveExerciseData(requestPayload)
        response.enqueue(object : Callback<ExerciseTrackingResponse> {
            override fun onResponse(
                call: Call<ExerciseTrackingResponse>,
                response: Response<ExerciseTrackingResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.Successful) {
                        Toast.makeText(
                            this@ExerciseActivity,
                            responseBody.Message,
                            Toast.LENGTH_LONG
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
                    this@ExerciseActivity,
                    "Failed to save exercise data !!!",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun loadLogInData(): LogInData {
        val preferences = getSharedPreferences(
            SignInActivity.LOGIN_PREFERENCE,
            SignInActivity.PREFERENCE_MODE
        )
        return LogInData(
            firstName = preferences.getString(SignInActivity.FIRST_NAME, "") ?: "",
            lastName = preferences.getString(SignInActivity.LAST_NAME, "") ?: "",
            patientId = preferences.getString(SignInActivity.PATIENT_ID, "") ?: "",
            tenant = preferences.getString(SignInActivity.TENANT, "") ?: ""
        )
    }

    private fun showExerciseInformation(context: Context, url: String) {
        val dialogView = LayoutInflater
            .from(context)
            .inflate(R.layout.image_popup_modal, null, false)
        val alertDialog = AlertDialog.Builder(context).setView(dialogView)
        val imageContainer: ImageView = dialogView.findViewById(R.id.gif_container)
        Glide.with(context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade(500))
            .override(600)
            .into(imageContainer)
        alertDialog.setPositiveButton("Okay") { _, _ ->
        }
        alertDialog.show()
    }
}
