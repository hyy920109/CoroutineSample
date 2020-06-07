### Kotlin协程（[Coroutine](https://www.kotlincn.net/docs/reference/coroutines/coroutines-guide.html) ）

#### 基础：

我们可以通过一下几种方式新启一个协程：

1.**GlobalScope.launch{}**

例如：

```kotlin
//global Scope 会启动一个新的非阻塞式的协程（会开启新的子线程）
GlobalScope.launch {
    delay(2000)
}
```

2.runBlocking {}

```kotlin
 		//阻塞式协程  由于没有另开线程 所以会导致阻塞当前线程 runBlocking会等待其协程以及所有子协程结束后 才会继续往下走
    	//实际上来看就是 runBlocking 只是常规函数 并没有做其他处理除了里面能够调用挂起函数外
        runBlocking {
             //这样子执行实际会导致主线程卡死2秒
       		//runBlocking会继承启动runBlocking的线程
        	delay(2000)
        }
```

#### 组合挂起函数：

默认顺序是串行执行 例如：

```kotlin
   private fun testConcatSuspendLinear() {
        //linear concat 顺序链接
        val i = runBlocking {
            println("start task")
            //测试执行完方法需要的时间  发现时间是叠加的
            val time = measureTimeMillis {
                val one = doSomethingUsefulOne()
                val two = doSomethingUsefulTwo()
                println("The answer is ${one + two}")
            }
            println("Completed in $time ms")
        }
    }
```

可以通过async 来改成并行

```kotlin
suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}
```

#### 协程上下文([CoroutineContext](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/) )和调度器([CoroutineDispatcher](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/index.html))

协程总是运行在CoroutineContext类型的上下文中，上下文一般都包含协程调度器（Dispatcher）， 协程调度器 则用来调度协程所在的线程。协程调度器有以下几种： **Dispatchers.IO   Dispatchers.Main  Dispatchers.Unconfined**

默认我们使用的launch{}   async{} 都是可以传入调度器的  用于指定线程，如果没加参数的话  则调度器（线程）继承自调用launch  async 的父协程。默认情况下一般都是父协程等待子协程（子作业）结束后父协程才算结束。





