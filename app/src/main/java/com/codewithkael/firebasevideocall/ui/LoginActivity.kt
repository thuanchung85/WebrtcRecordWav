package com.codewithkael.firebasevideocall.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

//@AndroidEntryPoint is an annotation provided by the Hilt library for enabling dependency injection in Android components. It simplifies the process of injecting dependencies into Android apps and reduces the amount of boilerplate code required for managing dependencies.
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    //ta dùng kỷ thuật view binding nên không cần dùng view findbyid nữa.
    // import class tạm ActivityLoginBinding là truy cập được activity_login.xml
    private lateinit var  views:ActivityLoginBinding

    @Inject lateinit var mainRepository: MainRepository

    //===on Create activity===//
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)

        //===code cho UI login activity===//
        views.apply {
            btnSignIn.setOnClickListener {
                //khi SignIn button click thi gọi mainRepository chạy task login
                mainRepository.login(usernameEt.text.toString(),passwordEt.text.toString())
                { isDone, reason ->
                    //nếu login success
                    if (isDone == true){
                        //start moving to our main activity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("username",usernameEt.text.toString())
                        })

                    }
                    //nếu login fail
                    else{
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }//end onCreate



}//end class