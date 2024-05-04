package com.codewithkael.firebasevideocall.utils

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

//========ĐÂY LÀ CỔNG CHÍNH CỦA APP "MAIN ACTIVITY"====///
//@HiltAndroidApp triggers Hilt's code generation,
// including a base class for your application that can use dependency injection.
//kích hoạt khả năng dependency injection cho toàn App bằng cách quy định đây là app thuộc dạng Hilt
@HiltAndroidApp
class MyApplication : Application()