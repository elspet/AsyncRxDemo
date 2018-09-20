package com.c4po.asyncrxdemo;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;

/**
 * @author Lisa
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private Button btnKnown;
    private ImageView ivShowImage;

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnKnown = findViewById(R.id.btn_unknown);
        ivShowImage = findViewById(R.id.iv_show_image);

        showRxMap();

        TimeDelay.delay(500, new TimeDelay.OnTimesUpListener() {
            @Override
            public void onTimesUp() {
                showRxFromArray();
            }
        });

        TimeDelay.delay(1000, new TimeDelay.OnTimesUpListener() {
            @Override
            public void onTimesUp() {
                showRxCompute();
            }
        });

        TimeDelay.delay(1500, new TimeDelay.OnTimesUpListener() {
            @Override
            public void onTimesUp() {
                showRxloadImage();
            }
        });


        TimeDelay.delay(2000, new TimeDelay.OnTimesUpListener() {
            @Override
            public void onTimesUp() {
                showRxSimple();
            }
        });

    }


    /**
     * 按次序将数组内容打印出来
     */
    private void showRxFromArray() {
        String[] name = {"Lisa", "Ann", "Elspet", "Jimbo"};
        Observable.fromArray(name)
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.d(TAG, "showRxFromArray - name " + s);
                    }
                });
    }


    private void showRxSimple() {
        String[] name = {"Lisa", "Ann", "Elspet", "Jimbo"};

        // 在子线程中打印人名
        Observable<String[]> observable = Observable.just(name);
        observable.subscribeOn(Schedulers.newThread())
                .subscribe(
                        new Consumer<String[]>() {
                            @Override
                            public void accept(String[] strings) throws Exception {
                                Log.d(TAG, "showRxSimple name = " + strings[0]);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.d(TAG, "error = " + throwable.getMessage());
                            }
                        });
    }


    /**
     * subscribeOn指的是Observable.onSubscribe()被激活时的线程
     * observeOn指的是事件消费线程，即Subscriber所运行的线程
     */
    private void showRxCompute() {

        // 在子线程中打印人名
        Observable<Integer> observable = Observable.just(compute(12, 22));
        observable
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Integer>() {
                            @Override
                            public void accept(Integer computeResult) throws Exception {
                                Log.d(TAG, "showRxCompute accept thread = " + Thread.currentThread().getName());
                                Log.d(TAG, "showRxCompute computeResult = " + computeResult);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.d(TAG, "error = " + throwable.getMessage());
                            }
                        });
    }


    /**
     * 计算函数发生在Observable.onSubscribe()中
     * 数据发送方
     * 由subscribeOn指定所在线程。
     *
     * @param a
     * @param b
     * @return
     */
    private int compute(int a, int b) {
        Log.d(TAG, "showRxCompute compute thread = " + Thread.currentThread().getName());
        return a * b;
    }

    /**
     * 加载图片，在IO线程中加载图片
     * 在主线程中展示
     */
    private void showRxloadImage() {
        final int drawableId = R.drawable.test;
        // 消息发送方
        Observable<Drawable> observable = Observable.create(new ObservableOnSubscribe<Drawable>() {
            @Override
            public void subscribe(ObservableEmitter<Drawable> emitter) throws Exception {

                String name = Thread.currentThread().getName();
                Log.d(TAG, "showRxloadImage subscribe currThread = " + name);
                Drawable drawable = MainActivity.this.getDrawable(drawableId);
                emitter.onNext(drawable);
            }
        });
        // 消息接收方
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                // 这个Consumer其实就是订阅者了
                .subscribe(new Consumer<Drawable>() {
                    @Override
                    public void accept(Drawable drawable) throws Exception {

                        String name = Thread.currentThread().getName();
                        Log.d(TAG, "showRxloadImage accept currThread = " + name);
                        ivShowImage.setImageDrawable(drawable);
                    }

                });

    }


    /**
     * map
     */
    private void showRxMap() {
        // 被观察者
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.d(TAG, "showRxMap - emitter thread = " + Thread.currentThread().getName());
                emitter.onNext("Lisa");
            }
        });

        Consumer<String> sub = new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.d(TAG, "showRxMap - subscriber thread = " + Thread.currentThread().getName());
                Log.d(TAG, "showRxMap - accept result = " + s);
            }

        };

        observable.subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.computation())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        Log.d(TAG, "showRxMap - map thread = " + Thread.currentThread().getName());
                        return s + " is working...";
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                // 很神奇，参数只支持Consumer
                .subscribe(sub);

    }

    static class TimeDelay{
        public static void delay(int timeSecond , final OnTimesUpListener listener){
            // Observable.timer
            Observable.timer(timeSecond, TimeUnit.MILLISECONDS)
                    .subscribe(
                            new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) throws Exception {
                                    listener.onTimesUp();
                                }
                            }
                    );
        }

        interface OnTimesUpListener{
            void onTimesUp();
        }
    }
}
