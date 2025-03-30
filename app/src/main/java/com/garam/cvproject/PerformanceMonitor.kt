package com.garam.cvproject

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 앱 성능 모니터링 및 프로파일링 유틸리티
 */
class PerformanceMonitor private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        
        // 싱글톤 인스턴스
        @Volatile
        private var INSTANCE: PerformanceMonitor? = null
        
        fun getInstance(context: Context): PerformanceMonitor {
            return INSTANCE ?: synchronized(this) {
                val instance = PerformanceMonitor(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
        
        // 백그라운드 모니터링 주기 (밀리초)
        private const val MONITOR_INTERVAL_MS = 5000L
        
        // 메모리 임계값 (MB)
        private const val MEMORY_THRESHOLD_MB = 200
    }
    
    // 모니터링 작업
    private var monitoringJob: Job? = null
    
    // 성능 이벤트 콜백
    private var perfListeners = mutableListOf<(PerformanceMetrics) -> Unit>()
    
    // 성능 측정 메트릭
    data class PerformanceMetrics(
        val usedMemoryMB: Int,
        val totalMemoryMB: Int,
        val cpuUsagePercent: Float,
        val frameTime: Long,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // 연산 실행 시간 측정
    private val operationTimes = mutableMapOf<String, Long>()
    
    /**
     * 연산 시작 시간 기록
     */
    fun startOperation(operationName: String) {
        operationTimes[operationName] = System.currentTimeMillis()
    }
    
    /**
     * 연산 종료 및 소요 시간 반환 (밀리초)
     */
    fun endOperation(operationName: String): Long {
        val startTime = operationTimes.remove(operationName) ?: return -1
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        Log.d(TAG, "Operation '$operationName' took $duration ms")
        return duration
    }
    
    /**
     * 현재 앱 메모리 사용량 측정 (MB)
     */
    fun getAppMemoryUsage(): Int {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemInBytes = runtime.totalMemory() - runtime.freeMemory()
        
        return (usedMemInBytes / (1024 * 1024)).toInt()
    }
    
    /**
     * 총 시스템 메모리 (MB)
     */
    fun getTotalSystemMemory(): Int {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        return (memoryInfo.totalMem / (1024 * 1024)).toInt()
    }
    
    /**
     * CPU 사용률 계산 (근사값)
     */
    private fun getCpuUsage(): Float {
        return try {
            val pid = android.os.Process.myPid()
            // Debug.ThreadInfo.currentThreadInfo() 대체
            val process = Runtime.getRuntime().exec("top -n 1 -p $pid")
            process.waitFor()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            
            // CPU 사용량 파싱 (근사값)
            val cpuPercent = output.lineSequence()
                .filter { it.contains("$pid") }
                .map { line -> 
                    line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        .getOrNull(8)?.toFloatOrNull() ?: 0f 
                }
                .firstOrNull() ?: 0f
                
            return cpuPercent
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating CPU usage", e)
            0f
        }
    }
    
    /**
     * 현재 프레임 시간 측정 (ms)
     */
    private var lastFrameTime = 0L
    private var frameTimeMs = 0L
    
    fun frameStart() {
        lastFrameTime = System.currentTimeMillis()
    }
    
    fun frameEnd() {
        if (lastFrameTime > 0) {
            frameTimeMs = System.currentTimeMillis() - lastFrameTime
        }
    }
    
    /**
     * 백그라운드 성능 모니터링 시작
     */
    fun startMonitoring(coroutineScope: CoroutineScope) {
        if (monitoringJob?.isActive == true) return
        
        monitoringJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                val metrics = collectMetrics()
                
                // 메모리 사용량이 임계값을 넘으면 로그 출력
                if (metrics.usedMemoryMB > MEMORY_THRESHOLD_MB) {
                    Log.w(TAG, "High memory usage: ${metrics.usedMemoryMB}MB")
                }
                
                // 이벤트 리스너에 알림
                perfListeners.forEach { it(metrics) }
                
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }
    
    /**
     * 모니터링 중지
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * 메트릭 데이터 수집
     */
    private fun collectMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            usedMemoryMB = getAppMemoryUsage(),
            totalMemoryMB = getTotalSystemMemory(),
            cpuUsagePercent = getCpuUsage(),
            frameTime = frameTimeMs
        )
    }
    
    /**
     * 성능 이벤트 리스너 등록
     */
    fun addPerformanceListener(listener: (PerformanceMetrics) -> Unit) {
        perfListeners.add(listener)
    }
    
    /**
     * 성능 이벤트 리스너 제거
     */
    fun removePerformanceListener(listener: (PerformanceMetrics) -> Unit) {
        perfListeners.remove(listener)
    }
    
    /**
     * 앱 시작부터 현재까지의 실행 시간 (초)
     */
    fun getAppUptime(): Long {
        // 앱 설치 시간 대신 앱 시작 시간 사용
        val startTime = if (startTimeMs == 0L) {
            System.currentTimeMillis().also { startTimeMs = it }
        } else {
            startTimeMs
        }
        
        return (System.currentTimeMillis() - startTime) / 1000
    }
    
    private var startTimeMs: Long = 0L
    
    /**
     * 메모리 힙 덤프 생성 (디버깅용)
     */
    fun createHeapDump(): String {
        return try {
            val heapDumpFile = context.getExternalFilesDir(null)?.absolutePath + "/heap_dump_${System.currentTimeMillis()}.hprof"
            Debug.dumpHprofData(heapDumpFile)
            "Heap dump created at $heapDumpFile"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create heap dump", e)
            "Failed to create heap dump: ${e.message}"
        }
    }
} 