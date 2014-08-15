package com.lfdb.parapesquisa.util;

/**
 * Created by igorlira on 08/09/13.
 */
public class Validator {
    public static boolean isEmailValid(String email) {
        String chunks[] = email.split("@");
        if(chunks.length != 2)
            return false;

        String user = chunks[0];
        String domain = chunks[1];
        if(domain.indexOf('.') < 0)
            return false;

        return true;
    }

    public static boolean isURLValid(String url) {
        return url.matches("((([A-Za-z]{3,9}:(?:\\/\\/)?)(?:[-;:&=\\+\\$,\\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\\+\\$,\\w]+@)[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??(?:[-\\+=&;%@.\\w_]*)#?(?:[.\\!\\/\\\\w]*))?)");
    }

    static int no(Character c) {
        return Integer.parseInt(c.toString());
    }

    public static boolean isCPFValid(String cpf) {
        if(cpf.length() != 11)
            return false;

        char buffer[] = cpf.toCharArray();

        int sum1 = no(buffer[0]) * 10 + no(buffer[1]) * 9 + no(buffer[2]) * 8 + no(buffer[3]) * 7 + no(buffer[4]) * 6 + no(buffer[5]) * 5 + no(buffer[6]) * 4 + no(buffer[7]) * 3 + no(buffer[8]) * 2;
        int dv1;
        if(sum1 % 11 >= 2)
            dv1 = 11 - (sum1 % 11);
        else
            dv1 = 0;

        int sum2 = no(buffer[0]) * 11 + no(buffer[1]) * 10 + no(buffer[2]) * 9 + no(buffer[3]) * 8 + no(buffer[4]) * 7 + no(buffer[5]) * 6 + no(buffer[6]) * 5 + no(buffer[7]) * 4 + no(buffer[8]) * 3 + dv1 * 2;
        int dv2;
        if(sum2 % 11 >= 2)
            dv2 = 11 - (sum2 % 11);
        else
            dv2 = 0;

        return no(buffer[9]) == dv1 && no(buffer[10]) == dv2;
    }
}
