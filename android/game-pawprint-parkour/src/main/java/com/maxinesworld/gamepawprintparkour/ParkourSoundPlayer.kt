package com.maxinesworld.gamepawprintparkour
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
class ParkourSoundPlayer(context:Context):AutoCloseable{
 private val pool=SoundPool.Builder().setMaxStreams(3).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
 private val jump=pool.load(context,R.raw.parkour_jump,1); private val token=pool.load(context,R.raw.parkour_token,1); private val bump=pool.load(context,R.raw.parkour_bump,1); private val finish=pool.load(context,R.raw.parkour_finish,1)
 fun jump()=play(jump,.45f);fun token()=play(token,.5f);fun bump()=play(bump,.35f);fun finish()=play(finish,.55f);private fun play(id:Int,v:Float){pool.play(id,v,v,1,0,1f)};override fun close()=pool.release()
}
