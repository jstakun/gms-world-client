package com.mnm.seekbarpreference;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.jstakun.gms.android.ui.lib.R;
import org.apache.commons.lang.StringUtils;

public final class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {

    // Namespaces to read attributes
    //private static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/com.jstakun.gms.android.ui";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    // Attribute names
    private static final String ATTR_DEFAULT_VALUE = "defaultValue";
    private static final String ATTR_MIN_VALUE = "minValue";
    private static final String ATTR_MAX_VALUE = "maxValue";
    // Default values for defaults
    private static final int DEFAULT_CURRENT_VALUE = 30;
    private static final int DEFAULT_MIN_VALUE = 30;
    private static final int DEFAULT_MAX_VALUE = 100;
    // Real defaults
    private final int mDefaultValue;
    private final int mMaxValue;
    private final int mMinValue;
    // Current value
    private int mCurrentValue;
    //unit of length
    // View elements
    private String unit;
    private SeekBar mSeekBar;
    private TextView mValueText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final String PREFERENCE_NS = "http://schemas.android.com/apk/res/" + context.getPackageName();
        
        // Read parameters from attributes
        mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
        mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
        mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE);
        unit = attrs.getAttributeValue(PREFERENCE_NS, "unit");
        if (unit == null) {
            unit = "";
        }
    }

    @Override
    protected View onCreateDialogView() {
        // Get current value from preferences
        mCurrentValue = getPersistedInt(mDefaultValue);

        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_slider, null);

        // Setup minimum and maximum text labels
        ((TextView) view.findViewById(R.id.min_value)).setText(setTextValue(mMinValue));
        ((TextView) view.findViewById(R.id.max_value)).setText(setTextValue(mMaxValue));

        // Setup SeekBar
        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        // Setup text label for current value
        mValueText = (TextView) view.findViewById(R.id.current_value);
        mValueText.setText(setTextValue(mCurrentValue));

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        // Persist current value if needed
        if (shouldPersist()) {
            persistInt(mCurrentValue);
        }

        // Notify activity about changes (to update preference summary line)
        notifyChanged();
    }

    @Override
    public CharSequence getSummary() {
        // Format summary string with current value
        String summary = super.getSummary().toString();
        int value = getPersistedInt(mDefaultValue);
        return String.format(summary, value, unit);
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        // Update current value
        mCurrentValue = value + mMinValue;
        // Update label with current value
        mValueText.setText(setTextValue(mCurrentValue));
    }

    public void onStartTrackingTouch(SeekBar seek) {
        // Not used
    }

    public void onStopTrackingTouch(SeekBar seek) {
        // Not used
    }
    
    private String setTextValue(int value) {
        String text = Integer.toString(value);
        if (StringUtils.isNotEmpty(unit)) {
            text += " " + unit;
        }
        return text;
    }
}