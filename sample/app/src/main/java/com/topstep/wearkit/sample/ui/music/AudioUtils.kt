package com.topstep.wearkit.sample.ui.music

import android.content.Context
import android.provider.MediaStore
import android.text.TextUtils
import io.reactivex.rxjava3.core.Observable

object AudioUtils {


    fun getLocalAudioFiles(context: Context): Observable<ArrayList<MusicBean>> {
       return Observable.create { emitter ->
            val localFileBeans = ArrayList<MusicBean>()
            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA
                    ),
                    MediaStore.Audio.Media.MIME_TYPE + "=? or "
                            + MediaStore.Audio.Media.MIME_TYPE + "=?", arrayOf("audio/mpeg"),
                    null
                )
                if (cursor!!.moveToFirst()) {
                    var musicBean: MusicBean?
                    do {
                        musicBean = MusicBean()
                        val fileName = cursor.getString(0)
                        val artist = cursor.getString(1)
                        val path = cursor.getString(2)
                        // singer name
                        if (!TextUtils.isEmpty(artist) && artist != "<unknown>") {
                            musicBean.singer = artist
                        }
                        musicBean.musicName = fileName
                        // file path
                        musicBean.path = path
                        if (!TextUtils.isEmpty(fileName) && (fileName.endsWith(".mp3") || fileName.endsWith(".MP3")) && !TextUtils.isEmpty(path)) {
                            localFileBeans.add(musicBean)
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                }
                emitter.onNext(localFileBeans)
                emitter.onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                emitter.onError(e)
            }
        }
    }
}