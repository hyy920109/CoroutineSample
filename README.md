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

#### 异步流

可以通过以下代码来构建一个流

```kotlin
val flow: Flow<Int> = flow {
    for (i in 1..3) {
        delay(500)
        //发射数据
        emit(i)
    }
}
```

通过以下代码来收集流的数据

```kotlin
flow.collect {value->
	println("value is $value")
}
```

##### 流（flow）是冷的，  flow 构建器的所有代码，只有在flow调用了collect 方法时候才会执行；

##### 流构建器(flow builder)：

通过flow{} 来构建是最基本的， 我们还可以通过以下方式来构建：

1.[flowOf](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flow-of.html) 构建器定义了一个发射固定值集的流。

2.使用 `.asFlow()` 扩展函数，可以将各种集合与序列转换为流

##### 流操作符

常见操作符有

1.[map](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/map.html)

2.[filter](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/filter.html)

3.[transform](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/transform.html)  

##### 终端操作符

collect 是终端操作符的最基本的一个，其他的还有 [toList](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/to-list.html)  [toSet](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/to-set.html)   [first](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/first.html)  [single](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/single.html)   [reduce](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/reduce.html) [fold](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/fold.html) 

##### 流的上下文

默认情况下flow遵循上下文保存属性，不允许从其他上下文中发射，不能通过withContext来切换上下文， 可以通过**flowOn**操作符来切换

##### 异常捕捉

通过[catch](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html)过渡操作符，两种方式捕捉异常：

```kotlin
fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    foo()
        .catch { e -> println("Caught $e") } // 不会捕获下游异常
        .collect { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
} 
```

上面得代码捕获不到collect{}代码块中的异常。

```kotlin
foo()
    .onEach { value ->
        check(value <= 1) { "Collected $value" }                 
        println(value) 
    }
    .catch { e -> println("Caught $e") }
    .collect()
```

将 [collect](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect.html) 操作符的代码块移动到 [onEach](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/on-each.html) 中，并将其放到 `catch` 操作符之前,这样我们就可以将 [catch](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html) 操作符的声明性与处理所有异常的期望相结合。

##### 流完成

流拥有 [onCompletion](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/on-completion.html) 过渡操作符，它在流完全收集时调用。



