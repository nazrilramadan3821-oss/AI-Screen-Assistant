package com.nazril.asisten;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

public class ShapeActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        final String[] nama = {"Robot (tangan & kaki)", "Bola bulat", "Robot kotak", "Robot 3D"};
        final int[] kode = {1, 0, 2, 3};
        new AlertDialog.Builder(this)
            .setTitle("Bentuk robot")
            .setItems(nama, (d, i) -> {
                getSharedPreferences("robot", MODE_PRIVATE)
                    .edit().putInt("shape", kode[i]).apply();
                finish();
            })
            .setOnCancelListener(d -> finish())
            .show();
    }
}
