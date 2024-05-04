package com.codewithkael.firebasevideocall.utils

import android.content.Context
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

//nơi đây là các khai báo về các module sẽ khỡi tạo trong toàn bộ lifecycle của app
//các modules được tạo ra để cho kỹ thuật  dependency injection có thể hoạt động.
//nếu không thì các constructor của dạng @Inject constructor sẽ không biết làm sao auto khỏi tạo các parameters của nó
@Module
@InstallIn(SingletonComponent::class)
//tất cả các module điều là SingletonComponent tồn tại sẳn trong RAM theo suốt APP để @Inject constructor gọi là có
class AppModule {

    //khỡi tạo object applicationContext() trong RAM và tồn tại vĩnh viễn
    @Provides
    fun provideContext(@ApplicationContext context:Context) : Context = context.applicationContext

    //khỡi tạo object Gson() trong RAM và tồn tại vĩnh viễn
    @Provides
    fun provideGson():Gson = Gson()

    //khỡi tạo object FirebaseDatabase() trong RAM và tồn tại vĩnh viễn
    @Provides
    fun provideDataBaseInstance():FirebaseDatabase = FirebaseDatabase.getInstance()

    //khỡi tạo object DatabaseReference() trong RAM và tồn tại vĩnh viễn
    @Provides
    fun provideDatabaseReference(db:FirebaseDatabase): DatabaseReference = db.reference
}