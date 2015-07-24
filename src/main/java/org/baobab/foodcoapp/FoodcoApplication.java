package org.baobab.foodcoapp;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.*;

@ReportsCrashes(
        mailTo = "flo@sonnenstreifen.de",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogTitle = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)

public class FoodcoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
    }
}
