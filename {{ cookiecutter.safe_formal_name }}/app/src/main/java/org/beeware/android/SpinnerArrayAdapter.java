package org.beeware.android;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SpinnerArrayAdapter extends ArrayAdapter<String>
implements android.widget.SpinnerAdapter
{
    Context context;
    private int textSize = 14; // initial default textsize
    private int textSizeUnit = TypedValue.COMPLEX_UNIT_SP;
    private Typeface typeface = null;
    private int typefaceStyle = Typeface.NORMAL;

    public SpinnerArrayAdapter(final Context context, final int resource) {
        super(context, resource);
        this.context = context;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(
                    android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        // android.R.id.text1 is default text view in resource of the android.
        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(getItem(position));
        tv.setTextSize(textSize);
        if (typeface == null) typeface = tv.getTypeface();
        else tv.setTypeface(typeface, typefaceStyle);
        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // android.R.layout.simple_spinner_item is default layout in resources of android.
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(
                    android.R.layout.simple_spinner_item, parent, false);
        }
        // android.R.id.text1 is default text view in resource of the android.
        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(getItem(position));
        tv.setTextSize(textSizeUnit, textSize);
        if (typeface == null) typeface = tv.getTypeface();
        else tv.setTypeface(typeface, typefaceStyle);
        return convertView;
    }

    //set the textSize
    public void setSpinnerTextSize(int unit, int size) {
        textSizeUnit = unit;
        textSize= size;
    }

    //set the TypeFace
    public void setSpinnerTypeFace(Typeface family, int style) {
        typeface = family;
        typefaceStyle = style;
    }

    //return the textSize
    public int getSpinnerTextSize() {
        return textSize;
    }

    //return the textSizeUnit
    public int getSpinnerTextSizeUnit() {
        return textSizeUnit;
    }

    //return the typeface
    public Typeface getSpinnerTypeface() {
        return typeface;
    }

    //return the typeface
    public int getSpinnerTypefaceStyle() {
        return typefaceStyle;
    }

}