# AsyncRxDemo
RxAndroid\RxJava学习与实践

###### 一、延时
```java
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
            /**
             * 计时时间到
             */
            void onTimesUp();
        }
    }
```

+ 调用
```java
        TimeDelay.delay(500, new TimeDelay.OnTimesUpListener() {
            @Override
            public void onTimesUp() {
                showRxFromArray();
            }
        });
```

###### 二、加载图片
```java
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
```

###### 三、依次打印数组内容
```java
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
```
