package com.example.plataformaremota

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator

object ShimmerHelper {

    fun animarRecursivo(view: View) {
        aplicar(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                animarRecursivo(view.getChildAt(i))
            }
        }
    }

    fun pararRecursivo(view: View) {
        (view.tag as? ObjectAnimator)?.cancel()
        view.tag = null
        view.alpha = 1f

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                pararRecursivo(view.getChildAt(i))
            }
        }
    }

    private fun aplicar(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "alpha", 0.4f, 1f, 0.4f)
        anim.duration = 1200
        anim.repeatCount = ObjectAnimator.INFINITE
        anim.repeatMode = ObjectAnimator.RESTART
        anim.interpolator = LinearInterpolator()
        anim.start()
        view.tag = anim
    }
}