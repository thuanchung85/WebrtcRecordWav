package com.codewithkael.firebasevideocall.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.GONE
import android.widget.Toast
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.firebase_repository.FireBaseMainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

//========ĐÂY LÀ CỔNG THỨ 2 CỦA APP "LOGIN ACTIVITY" LAUNCHER====///
//@AndroidEntryPoint is an annotation provided by the Hilt library for enabling dependency injection in Android components. It simplifies the process of injecting dependencies into Android apps and reduces the amount of boilerplate code required for managing dependencies.
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    //ta dùng kỷ thuật view binding nên không cần dùng view findbyid nữa.
    // import class tạm ActivityLoginBinding là truy cập được activity_login.xml
    private lateinit var  views:ActivityLoginBinding

    //khởi tạo firebase repository object để đi tới firebase, dùng @Inject để tạo constructor
    //cái hay là tạo constructor nhưng lại không new hay init nó, nó tự tạo ra luôn
    @Inject lateinit var fireBaseMainRepository: FireBaseMainRepository

    //===on Create activity===//
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)

        //===code cho UI login activity===//
        views.apply {
            //khởi tạo callback cho nut SignIn button, khi click thi gọi fireBaseMainRepository.login, truyền vào callback function nhận về result
            btnSignIn.setOnClickListener {
                if(usernameEt.text.toString().isEmpty() || passwordEt.text.toString().isEmpty()){
                   Toast.makeText(this@LoginActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else {
                    if(usernameEt.text.toString().length < 3 || passwordEt.text.toString().length < 5){
                        Toast.makeText(this@LoginActivity, "Username and password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    else {
                        //khi SignIn button click thi gọi mainRepository chạy task login
                        fireBaseMainRepository.login(
                            usernameEt.text.toString(),
                            passwordEt.text.toString()
                        )
                        { isDone, reason ->
                            //nếu login success
                            if (isDone == true) {
                                //start moving to our main activity
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        UsersShowActivity::class.java
                                    ).apply {
                                        putExtra("username", usernameEt.text.toString())
                                    })

                            }
                            //nếu login fail
                            else {
                                Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }
        }

    }//end onCreate



}//end class