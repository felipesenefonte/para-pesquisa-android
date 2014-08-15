package com.lfdb.parapesquisa.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Created by igorlira on 11/09/13.
 */
public class CurrencyControl implements TextWatcher {
    EditText txt;

    public CurrencyControl(EditText text)  {
        this.txt = text;
        text.addTextChangedListener(this);
    }

    public double getValue() {
        if(txt.getText().length() < 1)
            return -1;

        return Double.parseDouble(txt.getText().toString().replace(".", "").replace(",", "."));
    }

    public void setValue(double value) {
        String val = Double.toString(value).replace(".", ",");
        String integerValue, decimalValue, formattedValue;

        String chunks[] = val.split(",");
        integerValue = chunks[0];
        decimalValue = chunks[1];

        if(decimalValue.length() < 2)
            decimalValue = "0" + decimalValue;

        formattedValue = integerValue + decimalValue;
        txt.setText(formattedValue);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int _i, int i2, int i3) {
        if(charSequence.length() < 1)
            return;

        this.txt.removeTextChangedListener(this);

        String cleanValue = Long.toString(Long.parseLong(charSequence.toString().replace(".", "").replace(",", "")));
        String integerValue, decimalValue, formattedValue;

        if(cleanValue.length() >= 3) {
            integerValue = cleanValue.substring(0, cleanValue.length() - 2);
            decimalValue = cleanValue.substring(cleanValue.length() - 2, cleanValue.length());

            String splittedIntegerValue = "";
            for(int i = integerValue.length() - 1, j = 0; i >= 0; i--) {
                j++;

                char c = integerValue.charAt(i);

                splittedIntegerValue = c + splittedIntegerValue;

                if(j == 3 && i > 0) {
                    splittedIntegerValue = "." + splittedIntegerValue;
                    j = 0;
                }
            }
            formattedValue = splittedIntegerValue + "," + decimalValue;
        } else {
            if(cleanValue.length() < 2)
                cleanValue = "0" + cleanValue;

            formattedValue = "0," + cleanValue;
        }

        this.txt.setText(formattedValue);
        this.txt.setSelection(formattedValue.length());
        this.txt.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
}
