package com.codewithkael.firebasevideocall.firebaseClient

import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.LATEST_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.PASSWORD
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.STATUS
import com.codewithkael.firebasevideocall.utils.MyEventListener
import com.codewithkael.firebasevideocall.utils.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

//Singleton là 1 object được tạo ra tồn tại song song và duy nhất với toàn APP
//thường dùng tạo database object.
@Singleton
class FirebaseClient @Inject constructor(
    //áp dụng 1 singleton object DatabaseReference
    private val firebase_databaseRef:DatabaseReference,
    //áp dụng 1 singleton object Gson
    private val gson:Gson
) {

    private var currentUsername:String?=null
    private fun setUsername(username: String){
        this.currentUsername = username
    }


    //===đây là hàm login chính===//
    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        //addListenerForSingleValueEvent là lắng ghe value bi thay đổi trong database
        firebase_databaseRef.addListenerForSingleValueEvent(object  : MyEventListener(){
            override fun onDataChange(snapshot: DataSnapshot) {
                //if the current user exists
                if (snapshot.hasChild(username))
                {
                    //user exists , its time to check the password
                    val dbPassword = snapshot.child(username).child(PASSWORD).value
                    //nếu password OK
                    if (password == dbPassword)
                    {
                        //password is correct and sign in
                        //tác động vào username, thay đổi status của nó thành Online
                        firebase_databaseRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            //nếu thành công thay đổi data trên firebase database
                            .addOnCompleteListener {
                                //làm xong thì set user name hiện tại của app là username
                                setUsername(username)
                                //chạy callback function "done" bõ vào true
                                done(true,null)

                            }
                            //nếu fail thay đổi data trên firebase database
                            .addOnFailureListener {
                                done(false,"${it.message}")
                            }
                    }
                    //nếu sai password
                    else
                    {
                        //password is wrong, notify user
                        done(false,"Password is wrong")
                    }

                }
                //if user does not exists
                else
                {
                    //user doesn't exist, register the user
                    firebase_databaseRef.child(username).child(PASSWORD).setValue(password).addOnCompleteListener {
                        firebase_databaseRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUsername(username)
                                done(true,null)
                            }.addOnFailureListener {
                                done(false,it.message)
                            }
                    }.addOnFailureListener {
                        done(false,it.message)
                    }

                }
            }
        })
    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        firebase_databaseRef.addValueEventListener(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.filter { it.key !=currentUsername }.map {
                    it.key!! to it.child(STATUS).value.toString()
                }
                status(list)
            }
        })
    }

    fun subscribeForLatestEvent(listener:Listener){
        try {
            firebase_databaseRef.child(currentUsername!!).child(LATEST_EVENT).addValueEventListener(
                object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        val event = try {
                            gson.fromJson(snapshot.value.toString(),DataModel::class.java)
                        }catch (e:Exception){
                            e.printStackTrace()
                            null
                        }
                        event?.let {
                            listener.onLatestEventReceived(it)
                        }
                    }
                }
            )
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun sendMessageToOtherClient(message:DataModel, success:(Boolean) -> Unit){
        val convertedMessage = gson.toJson(message.copy(sender = currentUsername))
        firebase_databaseRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener {
                success(true)
            }.addOnFailureListener {
                success(false)
            }
    }

    fun changeMyStatus(status: UserStatus) {
        firebase_databaseRef.child(currentUsername!!).child(STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        firebase_databaseRef.child(currentUsername!!).child(LATEST_EVENT).setValue(null)
    }

    fun logOff(function:()->Unit) {
        firebase_databaseRef.child(currentUsername!!).child(STATUS).setValue(UserStatus.OFFLINE)
            .addOnCompleteListener { function() }
    }


    interface Listener {
        fun onLatestEventReceived(event:DataModel)
    }
}