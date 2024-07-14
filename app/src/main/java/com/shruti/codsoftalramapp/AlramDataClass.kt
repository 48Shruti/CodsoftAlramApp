package com.shruti.codsoftalramapp

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar

data class AlramDataClass(
    var id :String ?= "",
    var title : String ="",
    var time :String = "",
    val date : String = SimpleDateFormat("dd/MM/yy").format(Calendar.getInstance().time),
    var isAlarmOn: Boolean = false )
