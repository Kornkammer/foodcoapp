package org.baobab.foodcoapp;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

public class Barcode {

    public static void scan(final AppCompatActivity ctx, String mode) {
        try {
            Intent intent = new Intent(
                    "com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", mode);
            ctx.startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
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
    }
}
