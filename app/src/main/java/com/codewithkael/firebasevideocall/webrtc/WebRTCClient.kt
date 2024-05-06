package com.codewithkael.firebasevideocall.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WebRTCClient @Inject constructor(private val context: Context, private val gson: Gson)
{

    @Inject lateinit var serviceRepository: MainServiceRepository
    //class variables
    var listener: Listener? = null
    private lateinit var username: String

    //webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy {

        createPeerConnectionFactory()

    }.apply {
        if(adm != null) {
            adm.release();
        }
    }
    private var peerConnection: PeerConnection? = null
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
            .setUsername("83eebabf8b4cce9d5dbcb649")
            .setPassword("2D7JvfkOQtBdYW3R").createIceServer()
    )
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper:SurfaceTextureHelper?=null
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"))
    }

    // Executor thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    private val executor: ExecutorService? = Executors.newSingleThreadExecutor()

    //call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream? = null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack:AudioTrack?=null
    private var localVideoTrack:VideoTrack?=null

    //screen casting
    private var permissionIntent:Intent?=null
    private var screenCapturer:VideoCapturer?=null
    private val localScreenVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private var localScreenShareVideoTrack:VideoTrack?=null

    private var saveRecordedAudioToFile: RecordedAudioToFileController? = null
    private val appContext: Context = this.context
    val adm: AudioDeviceModule = createJavaAudioDevice()


    //installing requirements section
    init {

        initPeerConnectionFactory()

    }


    //===========


    fun createJavaAudioDevice() : AudioDeviceModule {
        // Enable/disable OpenSL ES playback.
        saveRecordedAudioToFile =  RecordedAudioToFileController(executor);
        val audioRecordErrorCallback = object : JavaAudioDeviceModule.AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(p0: String?) {
                Log.e("CHUNG", "onWebRtcAudioRecordInitError: $p0")
            }

            override fun onWebRtcAudioRecordStartError(p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?, p1: String?)
            {
                Log.e("CHUNG", "onWebRtcAudioRecordStartError: $p1")
            }

            override fun onWebRtcAudioRecordError(p0: String?) {
                Log.e("CHUNG", "onWebRtcAudioRecordError: $p0")
            }

        }
        val audioTrackErrorCallback = object : JavaAudioDeviceModule.AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(p0: String?) {
                Log.e("CHUNG", "onWebRtcAudioTrackInitError: $p0")
            }

            override fun onWebRtcAudioTrackStartError(
                p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                p1: String?
            ) {
                Log.e("CHUNG", "onWebRtcAudioTrackStartError: $p1")
            }

            override fun onWebRtcAudioTrackError(p0: String?) {
                Log.e("CHUNG", "onWebRtcAudioTrackError: $p0")
            }

        }
        val audioRecordStateCallback = object : JavaAudioDeviceModule.AudioRecordStateCallback{
                override fun onWebRtcAudioRecordStart() {
                    Log.i("CHUNG", "Audio recording onWebRtcAudioRecordStart");
                    if(saveRecordedAudioToFile != null) {
                        saveRecordedAudioToFile!!.start()

                        serviceRepository.changeTextCallActivy()
                    }
                }

                override fun onWebRtcAudioRecordStop() {
                    Log.i("CHUNG", "Audio recording onWebRtcAudioRecordStop");
                    if(saveRecordedAudioToFile != null) {
                        saveRecordedAudioToFile!!.stop()
                    }
                }

            }
        val audioTrackStateCallback = object : JavaAudioDeviceModule.AudioTrackStateCallback{
            override fun onWebRtcAudioTrackStart() {
                Log.i("CHUNG", "Audio playout onWebRtcAudioTrackStart");
            }

            override fun onWebRtcAudioTrackStop() {
                Log.i("CHUNG", "Audio playout onWebRtcAudioTrackStop");
            }

        }


        return JavaAudioDeviceModule.builder(appContext)
            .setSamplesReadyCallback(saveRecordedAudioToFile)
            .setSampleRate(16000)
            .setUseStereoInput(true)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecordStateCallback(audioRecordStateCallback)
            .setAudioTrackStateCallback(audioTrackStateCallback)
            .createAudioDeviceModule();
    }


    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)

            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory
            .initialize(options)

    }
    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBaseContext)
            ).setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBaseContext, true, true
                )
            ).setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = false
                disableEncryption = false
            }).createPeerConnectionFactory()
    }
    fun initializeWebrtcClient(
        username: String, observer: PeerConnection.Observer
    ) {
        this.username = username
        localTrackId = "${username}_track"
        localStreamId = "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    //negotiation section
    fun call(target:String){

        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Offer,
                            sender = username,
                            target = target,
                            data = desc?.description)
                        )
                    }
                },desc)
            }
        },mediaConstraint)
    }

    fun answer(target:String){
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(type = DataModelType.Answer,
                            sender = username,
                            target = target,
                            data = desc?.description)
                        )
                    }
                },desc)
            }
        },mediaConstraint)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription){
        peerConnection?.setRemoteDescription(MySdpObserver(),sessionDescription)
    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate){
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(target: String,iceCandidate: IceCandidate){
        addIceCandidateToPeer(iceCandidate)
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.IceCandidates,
                sender = username,
                target = target,
                data = gson.toJson(iceCandidate)
            )
        )
    }

    fun closeConnection(){
        try {
            videoCapturer.dispose()
            screenCapturer?.dispose()
            localStream?.dispose()
            peerConnection?.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun switchCamera(){
        videoCapturer.switchCamera(null)
    }

    fun toggleAudio(shouldBeMuted:Boolean){
        if (shouldBeMuted){
            localStream?.removeTrack(localAudioTrack)
        }else{
            localStream?.addTrack(localAudioTrack)
        }
    }

    fun toggleVideo(shouldBeMuted: Boolean){
        try {
            if (shouldBeMuted){
                stopCapturingCamera()
            }else{
                startCapturingCamera(localSurfaceView)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    //streaming section
    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }
    fun initRemoteSurfaceView(view:SurfaceViewRenderer){
        this.remoteSurfaceView = view
        initSurfaceView(view)
    }
    fun initLocalSurfaceView(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        this.localSurfaceView = localView
        initSurfaceView(localView)
        startLocalStreaming(localView, isVideoCall)
    }
    private fun startLocalStreaming(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if (isVideoCall){
            startCapturingCamera(localView)
        }

        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio",localAudioSource)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }
    private fun startCapturingCamera(localView: SurfaceViewRenderer){
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,eglBaseContext
        )

        videoCapturer.initialize(
            surfaceTextureHelper,context,localVideoSource.capturerObserver
        )

        videoCapturer.startCapture(
            720,480,20
        )

        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId+"_video",localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }
    private fun getVideoCapturer(context: Context):CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            }?:throw IllegalStateException()
        }
    private fun stopCapturingCamera(){

        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }

    //screen capture section

    fun setPermissionIntent(screenPermissionIntent: Intent) {
        this.permissionIntent = screenPermissionIntent
    }

    fun startScreenCapturing() {
       val displayMetrics = DisplayMetrics()
       val windowsManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowsManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels

        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,eglBaseContext
        )

        screenCapturer = createScreenCapturer()
        screenCapturer!!.initialize(
            surfaceTextureHelper,context,localScreenVideoSource.capturerObserver
        )
        screenCapturer!!.startCapture(screenWidthPixels,screenHeightPixels,15)

        localScreenShareVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId+"_video",localScreenVideoSource)
        localScreenShareVideoTrack?.addSink(localSurfaceView)
        localStream?.addTrack(localScreenShareVideoTrack)
        peerConnection?.addStream(localStream)

    }

    fun stopScreenCapturing() {
        screenCapturer?.stopCapture()
        screenCapturer?.dispose()
        localScreenShareVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localScreenShareVideoTrack)
        localScreenShareVideoTrack?.dispose()

    }

    private fun createScreenCapturer():VideoCapturer {
        return ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("permissions", "onStop: permission of screen casting is stopped")
            }
        })
    }


    interface Listener {
        fun onTransferEventToSocket(data: DataModel)
    }
}