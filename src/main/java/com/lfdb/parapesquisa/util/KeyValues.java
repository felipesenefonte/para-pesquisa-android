package com.lfdb.parapesquisa.util;

/**
 * Created by igorlira on 08/09/13.
 */
public class KeyValues {
    enum Token {
        Dummy,
        ObjectOpen,
        ObjectClose,
        String,
        Number,
        Unknown
    }

    char[] buffer;
    int pointer;

    public KeyValues(char[] buffer) {
        this.buffer = buffer;
        this.pointer = 0;
        this.parse();
    }

    Token getTokenSkipDummy(boolean consume) {
        while(getToken(false) == Token.Dummy) {
            pointer++;
        }

        return getToken(consume);
    }

    Token getToken(boolean consume) {
        char c = buffer[pointer];
        if(consume)
            pointer++;

        if(c == ' ' || c == '\t' || c == '\r' || c == '\n')
            return Token.Dummy;
        else if(c == '"')
            return Token.String;
        else if(c == '{')
            return Token.ObjectOpen;
        else if(c == '}')
            return Token.ObjectClose;
        else if(c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9' || c == '0')
            return Token.Number;

        return Token.Unknown;
    }

    String readString() {
        pointer++; // "
        String result = "";
        char c;
        while(true) {
            c = buffer[pointer++];
            if(c == '\\') {
                c = buffer[pointer++];
            } else if(c == '"') {
                pointer++;
                break;
            }

            result += c;
        }

        return result;
    }

    void parse() {
        String key = readString();
    }
}
