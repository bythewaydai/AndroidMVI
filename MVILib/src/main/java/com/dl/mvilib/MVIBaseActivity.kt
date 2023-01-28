package com.dl.mvilib

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class MVIBaseActivity<VB: ViewBinding> :AppCompatActivity() {

    protected  var viewBinding: VB?=viewBinding()

    fun viewBinding(): VB? {
        var vb:VB?=null
        val type = this.javaClass.genericSuperclass
        if (type is ParameterizedType) {
            //支持泛型
            try {
                //获取子类传过来的class信息，然后用class信息调用静态方法来实例化binding
                //binding=ActivityMainBinding.inflate(getLayoutInflater());
                //https://www.jianshu.com/p/e3d2421a0277
                val vbClass = type.actualTypeArguments[0] as Class<*> //ActivityMainBinding.class;
                val method = vbClass.getMethod("inflate", LayoutInflater::class.java)
                vb = method.invoke(null, layoutInflater) as VB
            } catch (e: Exception) {
                e.printStackTrace()
            }
            vb?.let {
                setContentView(it.root)
            }
        }
        return vb
    }
}


