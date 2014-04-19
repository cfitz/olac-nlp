// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   NlpResults.java

package org.olac.nlp;

import java.io.PrintStream;
import java.util.*;

public final class NlpResults
{

    public NlpResults()
    {
        dates = new ArrayList();
        people = new HashMap();
    }

    public NlpResults(ArrayList dates, HashMap people)
    {
        this.dates = dates;
        this.people = people;
    }

    public static void main(String args[])
    {
        NlpResults result = new NlpResults();
        result.dates.add("1978");
        HashSet roles = new HashSet();
        roles.add("father");
        roles.add("programer");
        result.people.put("chris fitzpatrick", roles);
        System.out.println(result.dates);
        System.out.println(result.people);
    }

    public ArrayList dates;
    public HashMap people;
}
