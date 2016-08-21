package org.baobab.foodcoapp;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

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
        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
            ACRA.getErrorReporter().addReportSender(new ReportSender() {
                @Override
                public void send(Context context, CrashReportData errorContent) throws ReportSenderException {
                    context.getSharedPreferences("crash", MODE_MULTI_PROCESS).edit()
                            .putBoolean("crashed", true).commit();
                }
            });
            ACRA.getErrorReporter().setEnabled(true);
        }
    }
}
