package com.nazril.asisten;

import android.content.Context;
import android.content.SharedPreferences;

public class RobotSettings {

    private static final String PREF = "robot_settings";

    private final SharedPreferences prefs;

    public RobotSettings(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public String getColor() {
        return prefs.getString("color", "blue");
    }

    public void setColor(String color) {
        prefs.edit().putString("color", color).apply();
    }

    public String getShape() {
        return prefs.getString("shape", "round");
    }

    public void setShape(String shape) {
        prefs.edit().putString("shape", shape).apply();
    }

    public String getExpression() {
        return prefs.getString("expression", "happy");
    }

    public void setExpression(String expression) {
        prefs.edit().putString("expression", expression).apply();
    }

    public int getSpeed() {
        return prefs.getInt("speed", 50);
    }

    public void setSpeed(int speed) {
        prefs.edit().putInt("speed", speed).apply();
    }

    public boolean isAutoExpression() {
        return prefs.getBoolean("auto_expression", true);
    }

    public void setAutoExpression(boolean enabled) {
        prefs.edit().putBoolean("auto_expression", enabled).apply();
    }
}
