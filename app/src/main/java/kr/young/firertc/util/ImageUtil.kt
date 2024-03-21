package kr.young.firertc.util

import android.net.Uri
import kr.young.firertc.util.Config.Companion.BACKGROUND
import kr.young.firertc.util.Config.Companion.IMAGE_LIST
import kr.young.firertc.vm.UserViewModel.Companion.defaultResource
import kr.young.firertc.vm.UserViewModel.Companion.profileImages

class ImageUtil {
    companion object {
        fun selectImage(id: String?, defaultRes: Int = defaultResource): Int {
            var img = defaultRes
            id?.let {
                img = profileImages[id.sumOf { it.code } % profileImages.size]
            }
            return img
        }

        fun selectImageFromWeb(id: String) = Uri.parse(IMAGE_LIST[(id.sumOf { it.code } + 1) % IMAGE_LIST.size])!!

        fun selectBackground(id: String) = Uri.parse(BACKGROUND[id.sumOf { it.code } % BACKGROUND.size])!!
    }
}