// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GenericPipeline.java

package org.olac.nlp;

import edu.stanford.nlp.pipeline.AnnotatorPool;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class GenericPipeline extends StanfordCoreNLP
{

    public GenericPipeline()
    {
    }

    public static void clearPool()
    {
        pool = null;
        System.gc();
    }

    private static AnnotatorPool pool = null;

}
