package com.example.coroutinedemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.coroutinedemo.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private var mainThreadId: Long = 0

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainThreadId = Thread.currentThread().id

        binding.globalScope.setOnClickListener {
            testGlobalScope()
        }

        binding.runBlocking.setOnClickListener {
            testRunBlocking()
        }

        binding.coroutineScope.setOnClickListener {
            runBlocking {
                testCoroutineScope()
            }
        }

        binding.cancelCoroutine.setOnClickListener {
            testCancelCoroutine()
        }

        binding.concatSuspend.setOnClickListener {
//            testConcatSuspendLinear()
            runBlocking {
                testConcatSuspendAsync()
            }
        }

        //调度器
        binding.dispatcher.setOnClickListener {
            testDispatchers()
        }

        //流  类似于Rxjava
        binding.flow.setOnClickListener {
            runBlocking {
                testFlowIO()
                //启动新的协程 以验证主线程并未阻塞
//                launch {
//                    for (i in 1..3) {
//                        println("I'm nor blocked $i")
//                        delay(500)
//                    }
//                }

//                withContext(Dispatchers.IO) {
//
//                }
            }


        }
    }

    suspend fun testFlowIO() {
        coroutineScope {
            testFlow().map {
                "response data is $it"
            }.collect { value ->
                println(value)
            }
        }
    }

    fun testFlow(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(500)
            emit(i)
        }
    }

    //子协程
    //当一个协程被其它协程在 CoroutineScope 中启动的时候， 它将通过 CoroutineScope.coroutineContext 来承袭上下文，
    // 并且这个新协程的 Job 将会成为父协程作业的 子 作业。当一个父协程被取消的时候，所有它的子协程也会被递归的取消。
    //然而，当使用 GlobalScope 来启动一个协程时，则新协程的作业没有父作业。 因此它与这个启动的作用域无关且独立运作。

    @ObsoleteCoroutinesApi
    private fun testDispatchers() {
        runBlocking {
            //launch 和 async 在构建新协程的的时候 可以接收一个可选的CoroutineContext 参数 它可以被显式的为一个新协程或其他上下文元素指定一个调度器
            //no dispatcher 没有指定调度器的话 默认会继承自父作用域的上下文以及调度器
            //当调用 launch { …… } 时不传参数，它从启动了它的 CoroutineScope 中承袭了上下文（以及调度器）。
            // 在这个案例中，它从 main 线程中的 runBlocking 主协程承袭了上下文。
            println("My job is ${coroutineContext[Job]}")
            launch {
                println("no Dispatchers I'm working in thread ${Thread.currentThread().name}")
            }

            //为协程指定名字
            launch(Dispatchers.IO + CoroutineName("hahah")) {

            }
            launch(Dispatchers.Unconfined) {
                println("Dispatchers.Unconfined I'm working in thread ${Thread.currentThread().name}")
            }
            launch(Dispatchers.IO) {
                println("Dispatchers.IO I'm working in thread ${Thread.currentThread().name}")
            }
            launch(Dispatchers.Default) {
                println("Dispatchers.Default I'm working in thread ${Thread.currentThread().name}")
            }

            //launch(newSingleThreadContext("MyOwnThread")) {
            // println("Dispatchers.Default I'm working in thread ${Thread.currentThread().name}")
            // }

            launch(Dispatchers.Main) {
                println("Dispatchers.Main I'm working in thread ${Thread.currentThread().name}")
            }
        }
    }

    //在概念上，async 就类似于 launch。它启动了一个单独的协程，
    // 这是一个轻量级的线程并与其它所有的协程一起并发的工作。
    // 不同之处在于 launch 返回一个 Job 并且不附带任何结果值，
    // 而 async 返回一个 Deferred —— 一个轻量级的非阻塞 future，
    // 这代表了一个将会在稍后提供结果的 promise。你可以使用 .await() 在一个延期的值上得到它的最终结果，
    // 但是 Deferred 也是一个 Job，所以如果需要的话，你可以取消它。

    //并发 挂起函数 让所有挂起函数能同时进行  async 可以通过将 start 参数设置为 CoroutineStart.LAZY 而变为惰性的
    //在惰性模式下，只有结果通过 await 获取的时候协程才会启动，或者在 Job 的 start 函数调用的时候
    private suspend fun testConcatSuspendAsync() {
        GlobalScope.launch(Dispatchers.IO) {
            println("start task")
            println("runBlocking thread id--->${Thread.currentThread().id}")
            launch {
                println("launch thread id--->${Thread.currentThread().id}")
                val time = measureTimeMillis {
                    val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
                    val two = async { doSomethingUsefulTwo() }
                    println("The answer is ${one.await() + two.await()}")
                }
                println("Completed in $time ms")
            }
        }
//        withContext(Dispatchers.IO) {
//            println("start task")
//            println("runBlocking thread id--->${Thread.currentThread().id}")
//            launch {
//                println("launch thread id--->${Thread.currentThread().id}")
//                val time = measureTimeMillis {
//                    val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
//                    val two = async { doSomethingUsefulTwo() }
//                    println("The answer is ${one.await() + two.await()}")
//                }
//                println("Completed in $time ms")
//            }
//
//        }
    }

    //组合挂起函数 默认顺序调用
    private fun testConcatSuspendLinear() {
        //linear concat 顺序链接
        val i = runBlocking {
            println("start task")
            val time = measureTimeMillis {
                val one = doSomethingUsefulOne()
                val two = doSomethingUsefulTwo()
                println("The answer is ${one + two}")
            }
            println("Completed in $time ms")
        }
    }

    private suspend fun doSomethingUsefulOne(): Int {
        println("start do one task")
        delay(1300L)
        return 12
    }

    private suspend fun doSomethingUsefulTwo(): Int {
        println("start do two task")
        delay(1000L)
        return 29
    }

    private fun testCancelCoroutine() {
        runBlocking {
            val startTime = System.currentTimeMillis()
            val job = launch(Dispatchers.Default) {
                var nextPrintTime = startTime
                var i = 0
                coroutineScope {
                    while (i < 5 && isActive) { // 一个执行计算的循环，只是为了占用 CPU
                        // 每秒打印消息两次
                        if (System.currentTimeMillis() >= nextPrintTime) {
                            println("job: I'm sleeping ${i++} ...")
                            nextPrintTime += 500L
                        }
                    }
                }
            }
            delay(1300)
            LogUtils.log(msg = "I'm tired of waiting!")
            job.cancelAndJoin()
            LogUtils.log(msg = "Now I can quit.")
        }
    }

    //runBlocking 与 coroutineScope 可能看起来很类似，因为它们都会等待其协程体以及所有子协程结束。
    //这两者的主要区别在于，runBlocking 方法会阻塞当前线程来等待，
    // 而 coroutineScope 只是挂起，会释放底层线程用于其他用途。
    //由于存在这点差异，runBlocking 是常规函数，而 coroutineScope 是挂起函数。
    private suspend fun testCoroutineScope() {
        LogUtils.log(msg = "runBlocking Thread id is ${Thread.currentThread().id} ")
        coroutineScope {
            delay(500)
            LogUtils.log(msg = "coroutineScope Thread id is ${Thread.currentThread().id} ")
//            launch {
//
//            }
        }
        LogUtils.log(msg = "Coroutine scope is over")
    }

    //阻塞式协程  由于没有另开线程 所以会导致阻塞当前线程 runBlocking会等待其协程以及所有子协程结束后 才会继续往下走
    //实际上来看就是 runBlocking 只是常规函数 并没有做其他处理
    private fun testRunBlocking() {
        //这样子执行实际会导致主线程卡死2秒
        //runBlocking会继承启动runBlocking的线程
        runBlocking {
            LogUtils.log(msg = "runBlocking Thread id is ${Thread.currentThread().id} ")
            testThreadId()
            //launch 和 Dispatchers 有关系
            launch { //
                LogUtils.log(msg = "launch Thread start id is ${Thread.currentThread().id} ")
                delay(3000)
                LogUtils.log(msg = "launch Thread end id is ${Thread.currentThread().id} ")

            }
            // delay(2000)
            LogUtils.log(msg = "Hello")
        }
        LogUtils.log(msg = "Run Blocking")
    }

    //global Scope 会启动一个新的非阻塞式的协程（会开启新的子线程）
    private fun testGlobalScope() {
        LogUtils.log(msg = "testGlobalScope Thread id is ${Thread.currentThread().id} ")
        GlobalScope.launch {
            //launch 也会启动一个新的非阻塞式的协程（会开启新的子线程）
            LogUtils.log(msg = "GlobalScope Thread id is ${Thread.currentThread().id} ")
            launch {
                LogUtils.log(msg = "launch Thread start id is ${Thread.currentThread().id} ")
                testThreadId()
                //这里的delay时长如果大于 globalScope的delay时长 那么任务结束后 会复用globalScape的线程
               // delay(1800)
                LogUtils.log(msg = "launch Thread end id is ${Thread.currentThread().id} ")
            }

            LogUtils.log(msg = "runBlocking Thread id is ${Thread.currentThread().id} ")

            delay(2000)
            LogUtils.log(msg = "GlobalScope")
        }
        LogUtils.log(msg = "Hello")
    }

    private suspend fun testThreadId() {
        LogUtils.log(msg = "test Thread start id is ${Thread.currentThread().id} ")
        delay(1000)
    }
}
