package com.codewithkael.firebasevideocall.ui

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
import com.codewithkael.firebasevideocall.databinding.ActivityMainBinding
import com.codewithkael.firebasevideocall.firebase_repository.FireBaseMainRepository
import com.codewithkael.firebasevideocall.service.MainService
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType
import com.codewithkael.firebasevideocall.utils.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

//========ĐÂY LÀ CỔNG THỨ 3 CỦA APP "MAIN USER VIEW ACTIVITY"====///
@AndroidEntryPoint
class UsersShowActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener {
    private val TAG = "UsersShowActivity"

    private lateinit var views: ActivityMainBinding
    private var username: String? = null

    @Inject
    lateinit var fireBaseMainRepository: FireBaseMainRepository
    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    private var mainAdapter: MainRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        //từ android sdk 33 thì phải vào setting cấp quyền ghi file băng tay
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(getpermission)
            }
        }

        ///XIN QUYEN MICRO//nếu micro ok quyền rôi thi hỏi tiep vi tri nguoi dung. và quyền đọc ghi file
        if (ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA, NOTIFICATION_SERVICE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 1123)
        }
        else{
            init()
        }
    }

    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 1123){
            init()
        }

    }

    private fun init() {
        username = intent.getStringExtra("username")
        if (username == null) finish()
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }

    private fun subscribeObservers() {
        setupRecyclerView()
        MainService.listener = this
        fireBaseMainRepository.observeUsersStatus {
            Log.d(TAG, "subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }
    }

    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        views.mainRecyclerView.apply {
            setLayoutManager(layoutManager)
            adapter = mainAdapter
        }
    }

    private fun startMyService() {
        mainServiceRepository.startService(username!!)
    }

    //====khi user click vào nút video call hình cái camera====//
    override fun onVideoCallClicked(username: String) {
        //check if permission of mic and camera is taken
        getCameraAndMicPermission {
            fireBaseMainRepository.sendConnectionRequest(username, true) {
                if (it){
                    //we have to start video call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this,CallActivity::class.java).apply {
                        putExtra("target",username)
                        putExtra("isVideoCall",true)
                        putExtra("isCaller",true)
                    })

                }
            }

        }
    }

    //====khi user click vào nút audio call hình cái điện thoại====//
    override fun onAudioCallClicked(username: String) {
        getCameraAndMicPermission {
            //gởi yêu cầu connect lên firebase
            fireBaseMainRepository.sendConnectionRequest(username, false) {
                //if firebase trả về là OK connect thi vào call activity
                if (it){
                    //we have to start audio call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this,CallActivity::class.java).apply {
                        putExtra("target",username)
                        putExtra("isVideoCall",false)

                        //nếu chính tôi là người gọi thì isCaller = true
                        putExtra("isCaller",true)

                    })
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }


    override fun onCallReceived(model: DataModel) {
        Log.d(TAG, "CHUNG onCallReceived ")
        runOnUiThread {
            views.apply {
                //hiện popup thông báo
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true

                //rung chuông ringring.wav
                val soundUri =
                    Uri.parse((ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName).toString() + "/" + R.raw.ringring)
                val mediaPlayer = MediaPlayer.create(this.root.context, soundUri)
                mediaPlayer.setVolume(0.1f, 0.1f)
                mediaPlayer.start()
                mediaPlayer.isLooping = true;

                //khi bấm nút accept call
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        //create an intent to go to video call activity
                        startActivity(Intent(this@UsersShowActivity,CallActivity::class.java).apply {
                            putExtra("target",model.sender)
                            putExtra("isVideoCall",isVideoCall)

                            //nếu  tôi là người nhận cuộc gọi thì isCaller = false
                            putExtra("isCaller",false)
                            putExtra("OK_Call_switch_to_calling_State","ok")
                        })

                        //stop ring
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                }

                //khi bấm nút decline call
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false

                    //stop ring
                    mediaPlayer.stop()
                    mediaPlayer.release()

                    //đây user kia về lại users page, gởi thông báo nào đó về việc từ chối nhận call
                    fireBaseMainRepository.sendEndCall()

                }

            }
        }
    }


}