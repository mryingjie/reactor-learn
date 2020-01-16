package com.demo.flux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * @Author ZhengYingjie
 * @Date 2019-07-31
 * @Description
 *
 *
 * 这几个方法调用，均返回包涵一个Boolean信号的Mono。
 *
 * all(Predicate<? super T> predicate)表示所有值均满足条件
 * any(Predicate<? super T> predicate)表示存在一个值满足条件
 * hasElement(T t)表示是否存在该值
 * hasElements()表示是否拥有一个或多个元素
 *
 * compose与as的区别是转化类型做了限制，必须继承Publisher，同时compose是惰性的。 都是转换用的类似map
 *
 */
public class FluxTest {


    @Before
    public void before(){
        Hooks.onOperatorDebug();
    }


    /**
     * Flux 和 Mono创建静态的数据  冷数据
     */
    @Test
    public void testCreateStaticData() {
        Flux<String> flux1 = Flux.just("one", "two", "three");
        Flux<String> flux2 = Flux.fromStream(Stream.of("one", "two", "three"));
        List<String> iterable = Arrays.asList("one", "two", "three");
        Flux<String> flux3 = Flux.fromIterable(iterable);
        Flux<Integer> flux4 = Flux.range(1, 3);
        //或者通过 #empty() 生成空数据
        Flux<String> fluxEmpty = Flux.empty();

        Mono<String> monoEmpty = Mono.empty();

        Mono<String> mono1 = Mono.just("one");

        //justOrEmpty 可以保证传入参数为空时也不会报错
        Mono<String> mono2 = Mono.justOrEmpty(null);
    }

    /**
     * Flux 和 Mono创建动态的数据 热数据
     */
    @Test
    public void testCreateDynamicData() {

        // generate 方法，在Flux中有3个重载方法，不管是哪个方法都是会包含一个循环构造函数。在每个循环中，sink.next()方法最多被调用一次。
        AtomicInteger num = new AtomicInteger(0);
        //案例1
        Flux.generate(
                num::get, //初始值 在方法中
                (value, sink) -> {
                    // 大于或等于10就执行完成  但是value已经等于10
                    if (++value >= 10) {
                        sink.complete();
                    }
                    //下发  这里从1开始 到9结束
                    sink.next(value);
                    return value; //迭代 需要返回值
                },
                integer -> {
                    System.out.println("last int = " + integer); //最后value的值 这里是10
                }
        );

        //案例2 生成的Flux一样
        Flux.generate(sink -> {
            if (num.incrementAndGet() >= 10) {
                sink.complete();
            }
            sink.next(num.get());
        });


        //案例3
        // create 方法，这个方法和 generate 类似，都是动态生成数据，但是数据生成的策略却恰恰相反，需要在一次方法中将全部数据生成。
        // 第二个参数是 create 方法 异步管理背压的策略，具体对 OverflowStrategy 的枚举如下。
        //
        // IGNORE 完全忽略下游背压请求。当下游队列满时，可能会产生 IllegalStateException。
        // ERROR 当下游跟不上数据的产生的时候，采用发送 IllegalStateException 来通知。
        // DROP 如果下游没有准备好接收信号，则丢弃输入信号。
        // LATEST 让下游只收到来自上游的最新信号。
        // BUFFER（默认）缓冲所有信号，如果下游跟不上。（这个无限制的缓冲可能导致OOM）。

        Flux<Integer> flux_create1 = Flux.create(sink -> {
            //这里的for循环也可以换成多线程并使用线程池来创建
            for (int i = 0; i < 10; i++) {
                sink.next(i);
            }
            sink.complete();
        }, FluxSink.OverflowStrategy.BUFFER);

        //interval 方法就是定时一段时间后产生数据，时间参数为 Duration。  这里从程序启动5秒钟后开始执行每两秒钟产生一条数据  从0开始每次递增1
        Flux<Long> interval = Flux.interval(Duration.of(5, ChronoUnit.SECONDS), Duration.of(2, ChronoUnit.SECONDS));
        foreachFlux(interval);

    }

    @Test
    public void testConcatMap(){
        Flux<List<Integer>> just = Flux.just(Arrays.asList(1, 2, 3), Arrays.asList(7, 8, 9), Arrays.asList(3, 4, 5));
        Flux<Object> objectFlux = just.concatMap(Flux::fromIterable);
        // [1, 2, 3]
        // [7, 8, 9]
        // [3, 4, 5]
        foreachFlux(just);
        // 1
        // 2
        // 3
        // 7
        // 8
        // 9
        // 3
        // 4
        // 5
        foreachFlux(objectFlux);

    }

    //与concatMap效果一样
    @Test
    public void testFlatMap(){
        Flux<List<Integer>> just = Flux.just(Arrays.asList(1, 2, 3), Arrays.asList(7, 8, 9), Arrays.asList(3, 4, 5));
        Flux<Object> objectFlux = just.flatMap(Flux::fromIterable);
        foreachFlux(objectFlux);
    }

    @Test
    public void testCollect(){
    }

    @Test
    public void testConcatWith(){
        Flux<List<Integer>> just = Flux.just(Arrays.asList(1, 2, 3), Arrays.asList(7, 8, 9), Arrays.asList(3, 4, 5));
        Flux<List<Integer>> listFlux = just.concatWith(just);
        foreachFlux(listFlux);
    }

    // handle() 方法可以类比为 Stream 的 map() + filter()
    @Test
    public void testHandle() {
        Flux<Object> handle = Flux.just(-1, 30, 13, 9, 20)
                .handle((value, sink) -> {
                            //将小于0 和大于20的过滤  并进行转换map操作
                            if (value > 0 && value < 20) {
                                sink.next((char) ('A' + value));
                            }

                        }
                );

        foreachFlux(handle);
    }

    // buff，缓存操作，作用就是将序列按照一定的序列进行缓存，达到一定数量的时候就将收集到的数据传递下去 这些数据都是 Collection 的实现，默认实现为 List）。
    // buffer(int maxSize, int skip) maxSize：缓存区大小。skip：当每个几个元素创建新的缓存块。  类似于SparkStreaming的窗口 maxSize是窗口大小 skip是滑动步长
    @Test
    public void testBuffer() {
        // 每隔一秒产生一条数据
        // Flux<Long> flux = Flux.interval(Duration.of(1, ChronoUnit.SECONDS));

        Flux<Long> flux = Flux.just(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        //无参将所有的数据封装为list返回 其实是调用buffer(Integer.MAX_VALUE)方法
        Flux<List<Long>> buffer1 = flux.buffer();

        foreachFlux(buffer1);

        // 调用 buffer 进行缓存，每隔2个开启缓存，每个缓存最大3个数据，并且只取前面3个缓存块
        Flux<List<Long>> buffer = flux.buffer(3, 2).take(3);


        //执行结果：
        // [0, 1, 2]
        // [2, 3, 4]
        // [4, 5, 6]
        // Subscription is completed!
        // foreachFlux(buffer);


        //给buffer增加超时时间 最大收集2个  如果一秒内还不够2个也会传递到下一级
        Flux<List<Long>> listFlux = flux.bufferTimeout(2, Duration.of(1, ChronoUnit.SECONDS));
        // [0, 1]
        // [2, 3]
        // [4, 5]
        // [6, 7]
        // [8]
        // foreachFlux(listFlux);

        //搜集到偶数就传递给下一级
        Flux<List<Long>> listFlux1 = flux.bufferUntil(i -> i % 2 == 0);
        // [0]
        // [1, 2]
        // [3, 4]
        // [5, 6]
        // [7, 8]
        // foreachFlux(listFlux1);

        //相当于过滤 将收集到的偶数都下发到下一级
        Flux<List<Long>> listFlux2 = flux.bufferWhile(i -> i % 2 == 0);
        // [0]
        // [2]
        // [4]
        // [6]
        // [8]
        // foreachFlux(listFlux2);
    }

    //window：这个方法和 buffer 方法类似，也有 windowTimeout、windowUntil、windowWhile，
    // 所有的用法都是一致的。只是在数据返回的时候不是 Collection，而是还是数据流也就是 Flux。
    @Test
    public void testWindow() {
        Flux<Long> flux = Flux.interval(Duration.of(1, ChronoUnit.SECONDS));
        //将原flux每三个数据重新封装为一个flux
        Flux<Flux<Long>> window = flux.window(3);
        foreachFlux(window);
    }

    //这个方法的主要功能就是数据流两两进行压缩，压缩函数默认采用 Flux.tuple2Function() 所以返回Tuple2，或者自动构建压缩方式  多余的数据被舍弃比如下面的3
    @Test
    public void testZip() {
        Flux<Tuple2<Integer, Integer>> tuple2Flux = Flux.just(0, 1).zipWith(Flux.just(2, 3, 4));
        // [0,2]
        // [1,3]
        // Subscription is completed!
        foreachFlux(tuple2Flux);

    }

    @Test
    public void testTake() {
        Flux<Integer> range = Flux.range(0, 10);

        // 获取数据中开头2个
        Flux<Integer> take = range.take(2);

        // 获取结尾的两个
        Flux<Integer> flux = range.takeLast(2);

        //While 当条件满足时就继续
        Flux<Integer> flux1 = range.takeWhile(i -> i < 5);
        // 0
        // 1
        // 2
        // 3
        // 4
        // Subscription is completed!
        // foreachFlux(flux1);


        //Util 当条件满足时，将之前的数据交给后续进行处理 后边的数据舍弃
        Flux<Integer> flux2 = range.takeUntil(i -> i < 5);
        // 0
        // Subscription is completed!
        // foreachFlux(flux2);

        //暂时没搞明白
        Flux<Integer> just = Flux.just(100);
        Flux<Integer> flux3 = range.takeUntilOther(just);
        foreachFlux(flux3);

    }

    // merge：用于将多个序列进行合并，当其中某个序列有消息就直接合并进去。
    // mergeSequential：同样是合并，只是按照顺序进行合并，只有前一个序列数据都输出完毕后，才开始处理之后的序列。
    @Test
    public void testMerge() {
        Flux<Long> flux1 = Flux.interval(Duration.of(1, ChronoUnit.SECONDS));
        Flux<Long> flux2 = Flux.interval(Duration.of(1, ChronoUnit.SECONDS));
        Flux<Integer> just = Flux.just(1, 2);

        Flux<Long> merge1 = Flux.merge(flux1, flux2);
        // foreachFlux(merge);

        Flux<? extends Number> merge2 = Flux.mergeSequential(just, flux1);
        foreachFlux(merge2);

    }

    @Test
    public void testError() {
        //
        Flux<Integer> error = Flux.just(1, 2)
                .concatWith(Mono.error(new IllegalStateException()));//1

        System.out.println("--------------1---------------");
        //处理方式1，直接通过订阅方式来分别输出处理
        error.subscribe(System.out::println, System.err::println);//2

        System.out.println("--------------2---------------");
        //通过 onErrorReturn 将异常信息转换成定义的好的特殊变量
        error.onErrorReturn(0).subscribe(System.out::println);//3

        System.out.println("--------------3---------------");
        //通过onErrorResume 将异常进行处理，同样返回定义好的特殊值
        error.onErrorResume(e -> {
            if (e instanceof IllegalStateException) {
                return Mono.just(0);
            } else if (e instanceof NullPointerException) {
                return Mono.just(-1);
            }
            return Mono.empty();
        })
                .subscribe(System.out::println);//4


    }

    //统计Flux中有几个数据 并返回一个Mono
    @Test
    public void testCount() {
        Flux<Integer> range = Flux.just(2, 2);

        Mono<Long> count = range.count();
        count.subscribe(System.out::println);
    }

    @Test
    public void testReduce() {
        //左闭右包 0-9
        Flux<Integer> range = Flux.range(0, 10);
        //求和
        Mono<Integer> reduce1 = range.reduce(Integer::sum);

        // 第一个参数为初始值
        Mono<ArrayList<Integer>> reduce2 = range.reduce(new ArrayList<>(), (integers, integer) -> {
            integers.add(integer);
            return integers;
        });

        reduce2.subscribe(System.out::println);
    }


    @Test
    public void testRepeat(){
        Flux<Integer> range = Flux.range(0, 10);
        Flux<Integer> repeat = range.repeat(); //无限复制原有的Flux 并放到新的Flux中 无界数据流

        Flux<Integer> repeat1 = range.repeat(2); //复制两次
        foreachFlux(repeat);
    }



    /**
     * 发布者都是一个 publish方法只会发布一次
     * 后面的订阅者只能从开始订阅的时间节点开始消费后边的数据
     * 订阅之前已经发布的数据将不会被后来的订阅者消费
     * 与replay方法的不同点在于 replay方法中后来的订阅者将从头开始重新将所有之前的数据消费一遍
     * @throws InterruptedException
     */
    @Test
    public void testPublish() throws InterruptedException {
        Flux fl = Flux.range(0,10000)
                .delayElements(Duration.ofSeconds(1))
                .publish(1) //prefetch 预取 不知道有啥用 0的时候
                .autoConnect();
        foreachFlux(fl);
        Thread.sleep(4000);
        foreachFlux(fl);
        Thread.sleep(4000L);
        foreachFlux(fl);

    }

    /**
     * 说明见testPublish()
     * @throws InterruptedException
     */
    @Test
    public void testReplay() throws InterruptedException {
        Flux<Integer> range = Flux.range(0, 10);
        Flux<Integer> flux1 = range.delayElements(Duration.ofSeconds(1))
                .replay()
                .autoConnect();
        foreachFlux(flux1);
        Thread.sleep(4000);
        foreachFlux(flux1);
    }


    /**
     * 之前说讲的 Flux、Mono 都只是异步数据序列，在没有订阅前，不会发生任何事情。不过在发布者中有两种数据类型，冷数据和热数据。
     * 上面说的都是指冷数据，他会为每个订阅者重新生成数据，如果没有订阅被创建，那么数据永远不会被生成。
     * 而热数据并不会依赖订阅者，它会在开始的时候就直接发布数据，
     * 当每一个新的订阅者过来，根据使用的订阅策略不同 将产生不同的效果 见testPublish() 和 testReplay()方法
     *
     */
    @Test
    public void testHotData() throws InterruptedException {
        //创建一个热数据源
        final UnicastProcessor<Integer> hotSource = UnicastProcessor.create();

        //定义数据处理逻辑
        Flux<Integer> flux = hotSource.replay() //使用replay方法 每个订阅者都将从头开始消费到完整的数据
        // Flux<Integer> flux = hotSource.publish() //使用publish方法 每个订阅者都将消费最新发布的和之后的数据
                .autoConnect()  //自动连接到注册进来的subscriber
                .map(x -> x * 10);

        // 新启一个线程定时发布数据
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                hotSource.onNext(i);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            hotSource.onComplete();
        }).start();

        //等待2秒后订阅
        Thread.sleep(2000L);
        // 等待2s后，启动第一个订阅者，这个订阅者可以拿到所有数据，因为之前没有订阅者，发布的数据都是缓存着的。
        flux.subscribe(x -> System.out.println("first subscriber consume " + x)
                , System.out::println
                , () -> System.out.println("completed!!!"));
        // 再启动一个订阅者，这个订阅者只能拿到发布者新发布的数据了。
        flux.subscribe(x -> System.out.println("second subscriber consume " + x));

        //在发布三秒的数据
        Thread.sleep(3000L);
        //取消数据源
        // 数据源进行了取消，之后发布的数据对会丢弃掉也不会执行OnComplete()，不会传递给订阅者。
        hotSource.cancel();

        hotSource.onNext(-1);

    }

    //transform运行程序将一系列操作链封装成一个函数，并且在调用的时候就会触发（点1），所以 2和3的结果一样
    @Test
    @SuppressWarnings("all")
    public void testTransform() throws InterruptedException {
        AtomicInteger ai = new AtomicInteger(0);
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {

            if (ai.incrementAndGet() == 1) {
                //当 ai=1 时  过滤出不等于“orange”的数据并将其转换为小写
                return f.filter(color -> !color.equals("orange"))
                        .map(String::toLowerCase);
            } else {
                //当 ai!=1 时 过滤出不等于“purple”的数据并将其装换为大写
                return f.filter(color -> !color.equals("purple"))
                        .map(String::toUpperCase);
            }
        };
        Flux<String> composedFlux =
                Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                        .transform(filterAndMap);// 1

        // blue
        // green
        // purple
        // Subscription is completed!
        foreachFlux(composedFlux); //2
        Thread.sleep(1000);
        // blue
        // green
        // purple
        // Subscription is completed!
        foreachFlux(composedFlux); //3
    }

    //compose，有点类似transform，也是将一系列的操作封装成函数。
    // 不过不会在调用的时候触发，只有订阅的时候才调用对应的函数，
    // 而且对每个订阅者都需要重新调用，所以点2和点3的数据是不一致的。
    @Test
    @SuppressWarnings("all")
    public void testCompose() throws InterruptedException {
        AtomicInteger ai = new AtomicInteger(0);
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {

            if (ai.incrementAndGet() == 1) {
                //当 ai=1 时  过滤出不等于“orange”的数据并将其转换为小写
                return f.filter(color -> !color.equals("orange"))
                        .map(String::toLowerCase);
            } else {
                //当 ai!=1 时 过滤出不等于“purple”的数据并将其装换为大写
                return f.filter(color -> !color.equals("purple"))
                        .map(String::toUpperCase);
            }
        };
        Flux<String> composedFlux =
                Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                        .compose(filterAndMap);// 1

        foreachFlux(composedFlux); //2
        Thread.sleep(1000);

        foreachFlux(composedFlux); //3

    }

    // publishOn 这个和别的函数操作符一致，都是从上游获取数据，先下游释放数据，并且方法会影响后续程序处所使用的调度任务，
    // 直到被后续别的 publishOn 方法锁定其他的调度任务。
    //
    // subscribeOn 应用于订阅流程，数据流开始所采用的调度任务就是这个方法来设定的。
    // 无论将这个方法在什么地方调用，总是会影响在链路开始的调度任务。
    //共同点在于 每个消费者都是单独的一个线程执行 只有多消费者的情况下才会有多线程执行
    @Test
    public void testPublishOnAndSubscribeOn() {
        Flux<String> map = Flux.range(0, 1000)
                .subscribeOn(Schedulers.immediate())
                // .publishOn(Schedulers.immediate())//当前线程执行
//                .publishOn(Schedulers.single())//另外启动一个线程执行
//                .publishOn(Schedulers.elastic()) //弹性执行 默认最大60个
                .publishOn(Schedulers.parallel()) //并行 获取cpu核数

                .map(x -> "[" + Thread.currentThread().getName() + "]" + ":" + x);

        foreachFlux(map);
        foreachFlux(map);
        foreachFlux(map);
        foreachFlux(map);
        foreachFlux(map);

    }

    /**
     * Flux 通过 parallel 方法可以生成 ParallelFlux。通过调用 subscribe 方法，
     * 来将订阅者进行分轨，再通过runOn来指定调度任务以提供足够的线程，
     * 在下面的例子中就将分成了4个线程来并行处理。
     * 注意如果没调用 runOn 就会使用当前线程，频繁的切换来处理订阅信息。
     * 与PublishOn And SubscribeOn 不同的是就算只有1个消费者都会采用多线程处理 即每个消费者都是多线程处理的
     */
    @Test
    public void testRunOn() {
        ParallelFlux<String> map = Flux.range(1, 10)
                .parallel(4)
                .runOn(Schedulers.parallel())
                .map(x -> "[" + Thread.currentThread().getName() + "]" + ":" + x);
        map.subscribe(x -> System.out.println("subscribe 1 ---->" + x));
        map.subscribe(x -> System.out.println("subscribe 2 ---->" + x));
        map.subscribe(x -> System.out.println("subscribe 3 ---->" + x));
        map.subscribe(x -> System.out.println("subscribe 4 ---->" + x));

    }


    //将数据按条件分组 相同分组的会被组装成GroupedFlux<Key,Value>
    @Test
    public void testGroupBy() {
        Flux<Integer> just = Flux.just(1, 2, 3, 4, 5);

        //按照groupBy方法传入的函数的返回值进行分组
        Flux<GroupedFlux<Boolean, Integer>> groupedFluxFlux = just.groupBy(integer -> integer % 2 == 0);
        Flux<Flux<List<Integer>>> map = groupedFluxFlux.map(Flux::buffer);


        foreachFlux(map);

    }


    private <T> void foreachFlux(Flux<T> buffer) {

        buffer.subscribe(
                v -> {
                    if (v instanceof Flux) {
                        foreachFlux((Flux<Object>) v);

                        return;
                    }

                    System.out.println(v);
                }, //数据处理 本质是onNext() 方法
                Throwable::printStackTrace,  //异常处理 onError() 方法
                () -> System.out.println("Subscription is completed!")); // 数据处理完成 onComplete()方法
    }

    @After
    public void after() {
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
