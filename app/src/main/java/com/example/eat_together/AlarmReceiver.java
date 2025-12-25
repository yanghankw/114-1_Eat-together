package com.example.eat_together;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 當系統計時時間到，會跑進來這裡
        NotificationHelper helper = new NotificationHelper(context);
        helper.showNotification("聚餐時間到！", "你設定的聚餐鬧鐘響囉，該出發了！");
    }
}