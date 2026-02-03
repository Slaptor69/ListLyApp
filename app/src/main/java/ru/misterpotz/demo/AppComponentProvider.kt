//удобный способ достать уже созданный AppComponent из любого места, где есть Context
package ru.misterpotz.demo

import android.content.Context
import ru.misterpotz.demo.di.AppComponent

val Context.appComponent: AppComponent
    get() = (applicationContext as MyApplication).let { MyApplication.component }
