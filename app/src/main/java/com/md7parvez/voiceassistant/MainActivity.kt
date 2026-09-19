package com.md7parvez.voiceassistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*

class MainActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var status: TextView; private lateinit var transcript: TextView; private lateinit var indicator: TextView
    private lateinit var speech: SpeechInputManager; private lateinit var output: SpeechOutputManager; private lateinit var sensors: SensorManagerService
    private val engine = LocalAssistantEngine(); private var state = AssistantState.IDLE; private var xplore = false
    private var surface: SurfaceView? = null; private var camera: CameraDevice? = null
    private val micCode = 10; private val cameraCode = 11
    override fun onCreate(saved: Bundle?) { super.onCreate(saved); speech = SpeechInputManager(this); output = SpeechOutputManager(this); sensors = SensorManagerService(this); buildMain() }
    private fun buildMain() { val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,20,24,20); setBackgroundColor(Color.rgb(16,20,24)) }
        fun tv(text: String, size: Float) = TextView(this).apply { this.text=text; textSize=size; setTextColor(Color.WHITE); setPadding(4,8,4,8) }
        root.addView(tv("VoiceAssistant", 26f)); indicator = tv("●  IDLE", 30f).apply { gravity=1; setTextColor(Color.rgb(128,203,196)) }; root.addView(indicator, LinearLayout.LayoutParams(-1,0,1f))
        status=tv("Ready",14f); root.addView(status); transcript=tv("Conversation will appear here.",16f); root.addView(transcript, LinearLayout.LayoutParams(-1,0,1f))
        val mic=Button(this).apply { text="🎙  Speak"; setOnClickListener { startVoice() } }; root.addView(mic)
        val explore=Button(this).apply { text="Open Xplore Mode"; setOnClickListener { openXplore() } }; root.addView(explore); setContentView(root) }
    private fun startVoice() { if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),micCode); return }; state=AssistantState.LISTENING; render(); speech.start(object: SpeechInputManager.Listener { override fun onListening(){ status.text="Listening…" }; override fun onResult(text:String){ transcript.text="You: $text"; state=AssistantState.PROCESSING; render(); val response=engine.processUserInput(text,AssistantContext(sensors.summary(),xplore)); transcript.append("\n\nAssistant: $response"); output.speak(response,{state=AssistantState.SPEAKING;render()},{state=if(xplore) AssistantState.XPLORE else AssistantState.IDLE;render()}) }; override fun onError(message:String){ status.text=message; state=AssistantState.ERROR; render() } }) }
    private fun render(){ indicator.text="●  ${state.name}"; status.text=when(state){AssistantState.LISTENING->"Listening…";AssistantState.PROCESSING->"Thinking…";AssistantState.SPEAKING->"Speaking…";AssistantState.XPLORE->"Xplore Mode active";AssistantState.ERROR->"An error occurred";else->"Ready"} }
    private fun openXplore(){ if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CAMERA),cameraCode);return}; xplore=true; state=AssistantState.XPLORE; val root=FrameLayout(this); surface=SurfaceView(this); surface!!.holder.addCallback(this); root.addView(surface,FrameLayout.LayoutParams(-1,-1)); val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}; panel.addView(TextView(this).apply{text="Xplore Mode";textSize=24f;setTextColor(Color.WHITE)}); panel.addView(Button(this).apply{text="Exit Xplore";setOnClickListener{closeXplore()}}); root.addView(panel); setContentView(root) }
    private fun closeXplore(){ camera?.close();camera=null;xplore=false;state=AssistantState.IDLE;buildMain() }
    override fun surfaceCreated(holder: SurfaceHolder){ try { val manager=getSystemService(CAMERA_SERVICE) as CameraManager; val id=manager.cameraIdList.firstOrNull() ?: throw Exception("Camera unavailable"); manager.openCamera(id,object:CameraDevice.StateCallback(){override fun onOpened(c:CameraDevice){camera=c;c.createCaptureSession(listOf(holder.surface),object:CameraCaptureSession.StateCallback(){override fun onConfigured(s:CameraCaptureSession){s.setRepeatingRequest(c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply{addTarget(holder.surface)}.build(),null,null)}override fun onConfigureFailed(s:CameraCaptureSession){status?.text="Camera preview failed"}},null)}override fun onDisconnected(c:CameraDevice){c.close()}override fun onError(c:CameraDevice,e:Int){c.close()}},null) }catch(e:Exception){status?.text="Camera unavailable"} }
    override fun surfaceChanged(h:SurfaceHolder,f:Int,w:Int,hg:Int)=Unit; override fun surfaceDestroyed(h:SurfaceHolder){camera?.close();camera=null}
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==micCode&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)startVoice() else if(r==cameraCode&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)openXplore() else status?.text="Permission denied; feature unavailable"}
    override fun onDestroy(){speech.stop();output.release();sensors.stop();camera?.close();super.onDestroy()}
}
