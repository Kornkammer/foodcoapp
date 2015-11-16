package org.baobab.foodcoapp.util;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

public class Barcode {

    public static void scan(final Fragment fragment, String mode) {
        try {
            fragment.startActivityForResult(scanIntent(mode), 0);
        } catch (ActivityNotFoundException e) {
            promptForScannerInstall(fragment.getActivity());
        }
    }

    public static void scan(final AppCompatActivity ctx, String mode) {
        try {
            ctx.startActivityForResult(scanIntent(mode), 0);
        } catch (ActivityNotFoundException e) {
            promptForScannerInstall(ctx);
        }
    }

    private static void promptForScannerInstall(final Context ctx) {
        new AlertDialog.Builder(ctx)
                .setMessage("Kein QR Scanner installiert")
                .setNegativeButton("Schade", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Jetz installieren", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "market://details?id=com.google.zxing.client.android"
                        )));
                        dialog.dismiss();
                    }
                }).show();
    }

    @NonNull
    private static Intent scanIntent(String mode) {
        Intent intent = new Intent(
                "com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", mode);
        return intent;
    }
}
