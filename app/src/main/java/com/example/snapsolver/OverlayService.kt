package com.example.snapsolver

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.window.SplashScreen
import kotlin.system.exitProcess
import android.graphics.Color

class OverlayService : Service() {

    companion object {
        var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) {
            // 이미 실행 중이면 아무것도 하지 않고 그냥 return
            return START_STICKY
        }
        isRunning = true

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 메인 레이아웃을 가로 방향으로 변경
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = null
            gravity = Gravity.CENTER_VERTICAL
        }

        // 드래그 핸들 영역 생성
        val dragHandle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                100, 100
            )
            setBackgroundColor(0x55888888) // 약간 보이는 회색
        }

        // 버튼 상태를 추적하기 위한 변수
        var buttonState = 0 // 0: 캡쳐(노란색), 1: 시작(녹색), 2: 중단(빨간색)

        // 버튼 스타일을 설정하는 함수
        fun createMinimalButton(text: String): Button {
            return Button(this).apply {
                this.text = text
                this.textSize = 12f // 텍스트 크기 줄이기

                // 버튼 스타일 직접 설정
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4) // 상하 여백 추가
                }

                // 버튼 크기 최소화
                minWidth = 100
                minHeight = 100
                minimumWidth = 100
                minimumHeight = 100

                // 패딩 최소화
                setPadding(2, 2, 2, 2)

                // 배경 스타일 조정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    elevation = 0f // 그림자 제거
                }
            }
        }

        // 첫 번째 버튼 생성 - 처음에는 "캡쳐" 상태로 시작
        val actionButton = createMinimalButton("캡쳐").apply {
            // 초기 상태는 노란색
            setBackgroundColor(Color.YELLOW)
            setTextColor(Color.BLACK) // 노란색 배경에는 검은색 텍스트

            setOnClickListener {
                buttonState = (buttonState + 1) % 3

                when (buttonState) {
                    0 -> { // 캡쳐 상태
                        text = "캡쳐"
                        setBackgroundColor(Color.YELLOW)
                        setTextColor(Color.BLACK) // 노란색 배경에는 검은색 텍스트
                        Toast.makeText(this@OverlayService, "캡쳐 모드", Toast.LENGTH_SHORT).show()
                    }
                    1 -> { // 시작 상태
                        text = "시작"
                        setBackgroundColor(Color.GREEN)
                        setTextColor(Color.BLACK) // 녹색 배경에는 검은색 텍스트
                        Toast.makeText(this@OverlayService, "녹화 시작", Toast.LENGTH_SHORT).show()
                    }
                    2 -> { // 중단 상태
                        text = "중단"
                        setBackgroundColor(Color.RED)
                        setTextColor(Color.WHITE) // 빨간색 배경에는 흰색 텍스트
                        Toast.makeText(this@OverlayService, "녹화 중단", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val stopButton = createMinimalButton("종료").apply {
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)

            setOnClickListener {
                windowManager.removeView(overlayView)
                stopSelf()
                exitProcess(0)
            }
        }

        layout.addView(dragHandle)
        layout.addView(actionButton)
        layout.addView(stopButton)

        // 드래그 핸들에만 터치 리스너 추가
        dragHandle.setOnTouchListener { view, event ->
            val params = overlayView.layoutParams as WindowManager.LayoutParams

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(overlayView, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                else -> false
            }
        }

        // 윈도우 매니저에 추가
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        overlayView = layout
        windowManager.addView(overlayView, params)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
