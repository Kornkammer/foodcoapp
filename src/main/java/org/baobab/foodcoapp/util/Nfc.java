package org.baobab.foodcoapp.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.baobab.foodcoapp.AccountActivity;
import org.ndeftools.MimeRecord;
import org.ndeftools.Record;

import java.io.IOException;
import java.util.List;

public class Nfc {

    public static void resume(Context ctx, String action, String mime) {
        try {
            NfcAdapter nfc = NfcAdapter.getDefaultAdapter(ctx);
            if (nfc == null) return;
            IntentFilter f = new IntentFilter(action);
            if (mime != null) {
                f.addDataType(mime);
            }
            PendingIntent pi = PendingIntent.getActivities(ctx, 1,
                    new Intent[] { new Intent(ctx, ctx.getClass()) }, 0);

            nfc.enableForegroundDispatch(
                    (AppCompatActivity) ctx, pi, new IntentFilter[]{f}, null);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
    };

    public static void pause(Context ctx) {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(ctx);
        if (nfc == null) return;
        nfc.disableForegroundDispatch((AppCompatActivity) ctx);
    }

    public static boolean writeTag(Intent intent, String msg) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] textBytes = msg.getBytes();
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                "application/vnd.kornkammer.products".getBytes(), new byte[] {}, textBytes);
        NdefMessage m = new NdefMessage(new NdefRecord[]{ textRecord });
        return writeTag(m, tag);
    }

    public static String readTag(Intent intent) {
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (messages != null) {
                try {
                    List<Record> records = new org.ndeftools.Message((NdefMessage)messages[0]);
                    MimeRecord record = (MimeRecord) records.get(0);
                    return new String(record.getNdefRecord().getPayload());
                } catch (Exception e) {
                    Log.e(AccountActivity.TAG, "error reading tag: " + e.getMessage());
                }
            }
        return null;
    }

    public static boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}
