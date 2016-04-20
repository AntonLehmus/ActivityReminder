package fi.antonlehmus.activityreminder;

//https://stackoverflow.com/questions/5533078/timepicker-in-preferencescreen

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class TimePreference extends DialogPreference {
    private Calendar calendar;
    private TimePicker picker = null;

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText(R.string.set);
        setNegativeButtonText(R.string.cancel);
        calendar = new GregorianCalendar();
    }

    //creates TimePicker
    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        return (picker);
    }

    //Set current time as default value for TimePicker
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        //This method was deprecated in API level 23. Use setHour(int)
        picker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        picker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }

    //Deal with results
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            calendar.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
            calendar.set(Calendar.MINUTE, picker.getCurrentMinute());

            setSummary(getSummary());
            if (callChangeListener(getCurrentMillis(calendar))) {
                persistLong(getCurrentMillis(calendar));
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        //Log.d("TimePreference","onSetInitialValue");
        //Log.d("TimePreference","restoreValue="+restoreValue);
        if (restoreValue) {
            if (defaultValue == null) {
                //Log.d("TimePreference","defaultValue=null");
                calendar.setTime(parseDate(getPersistedLong(getCurrentMillis(calendar))));
            } else {
                calendar.setTime(parseDate(Long.parseLong(getPersistedString((String) defaultValue))));
            }
        } else {
            if (defaultValue == null) {
                //Log.d("TimePreference","defaultValue=null");
                calendar.setTime(parseDate(getCurrentMillis(calendar)));
            } else {
                calendar.setTime(parseDate(Long.parseLong((String) defaultValue)));
            }
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        if (calendar == null) {
            return null;
        }
        return DateFormat.getTimeFormat(getContext()).format(new Date(calendar.getTimeInMillis()));
    }

    //returns milliseconds of calendar object from start of the day
    private long getCurrentMillis(Calendar calendar){
        long sum = 0;

        sum= TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY))+
                TimeUnit.MINUTES.toMillis(calendar.get(Calendar.MINUTE))+
                TimeUnit.SECONDS.toMillis(calendar.get(Calendar.SECOND))+
                calendar.get(Calendar.MILLISECOND);
        return sum;
    }

    private Date parseDate (long millisFromDayStart){
        Calendar calendar = Calendar.getInstance();
        Date date;
        long millis= millisFromDayStart;

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis = millis - TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis = millis - TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis = millis - TimeUnit.SECONDS.toMillis(seconds);

        calendar.set(Calendar.HOUR_OF_DAY,(int)hours);
        calendar.set(Calendar.MINUTE,(int)minutes);
        calendar.set(Calendar.SECOND,(int)seconds);
        calendar.set(Calendar.MILLISECOND,(int)millis);
        /*
        Log.d("TimePreference","parsed: "+calendar.get(Calendar.HOUR_OF_DAY)+":"+
                calendar.get(Calendar.MINUTE)+":"+
                calendar.get(Calendar.SECOND)+":"+millis);
        */

        date = calendar.getTime();

        return date;
    }
}