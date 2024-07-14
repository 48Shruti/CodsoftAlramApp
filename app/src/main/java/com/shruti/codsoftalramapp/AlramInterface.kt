package com.shruti.codsoftalramapp

import android.icu.text.Transliterator.Position

interface AlramInterface {
    fun setAlarm(alramDataClass: AlramDataClass,position: Int)
    fun cancelAlarm(alramDataClass: AlramDataClass,position: Int)
    fun update(alramDataClass: AlramDataClass, position: Int)

}