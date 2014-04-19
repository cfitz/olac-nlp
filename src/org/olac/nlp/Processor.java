// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Processor.java

package org.olac.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import org.apache.lucene.queryParser.ParseException;

// Referenced classes of package org.olac.nlp:
//            RoleExtraction, GenericPipeline, NlpResults

public class Processor
{

    public Processor()
        throws IOException, ParseException
    {
        roleExtractor = new RoleExtraction();
        warmup();
    }

    public Processor(HashMap roles)
        throws IOException, ParseException
    {
        roleExtractor = new RoleExtraction(roles);
        warmup();
    }

    public Processor(ArrayList names)
        throws IOException, ParseException
    {
        roleExtractor = new RoleExtraction(names);
        warmup();
    }

    public Processor(HashMap roles, ArrayList names)
        throws IOException, ParseException
    {
        roleExtractor = new RoleExtraction(roles, names);
        warmup();
    }

    public void nullify()
        throws Throwable
    {
        pipeline = null;
        GenericPipeline.clearPool();
        System.gc();
    }

    public void warmup()
    {
        if(pipeline == null)
            pipeline = new GenericPipeline();
    }

    private Annotation annotateDocument(String text)
    {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        return document;
    }

    public NlpResults process(String text)
        throws IOException
    {
        warmup();
        NlpResults results = new NlpResults();
        ArrayList dates = new ArrayList();
        ArrayList people = new ArrayList();
        Annotation document = annotateDocument(text);
        List sentences = (List)document.get(edu/stanford/nlp/ling/CoreAnnotations$SentencesAnnotation);
        for(Iterator iterator = sentences.iterator(); iterator.hasNext();)
        {
            CoreMap sentence = (CoreMap)iterator.next();
            Integer endIndex = Integer.valueOf(0);
            for(Iterator iterator1 = ((List)sentence.get(edu/stanford/nlp/ling/CoreAnnotations$TokensAnnotation)).iterator(); iterator1.hasNext();)
            {
                CoreLabel token = (CoreLabel)iterator1.next();
                String ne = (String)token.get(edu/stanford/nlp/ling/CoreAnnotations$NamedEntityTagAnnotation);
                if(ne.equals("DATE"))
                {
                    String nerDate = (String)token.get(edu/stanford/nlp/ling/CoreAnnotations$NormalizedNamedEntityTagAnnotation);
                    int index = nerDate.indexOf("X");
                    if(index < 0 && !nerDate.contains("OFFSET"))
                        dates.add(nerDate);
                } else
                if(ne.equals("PERSON"))
                {
                    Integer startIndex = Integer.valueOf(token.beginPosition() - 1);
                    if(startIndex.intValue() - endIndex.intValue() == 0 && people.size() > 0)
                    {
                        Integer last = Integer.valueOf(people.size() - 1);
                        String newName = ((String)people.get(last.intValue())).concat(" ").concat((String)token.get(edu/stanford/nlp/ling/CoreAnnotations$TextAnnotation));
                        people.set(last.intValue(), newName);
                    } else
                    {
                        people.add((String)token.get(edu/stanford/nlp/ling/CoreAnnotations$TextAnnotation));
                    }
                    endIndex = Integer.valueOf(token.endPosition());
                }
            }

        }

        HashMap peopleAndRoles = new HashMap();
        try
        {
            peopleAndRoles = roleExtractor.extractRoles(people, text);
        }
        catch(ParseException e)
        {
            throw new IOException(e.getMessage());
        }
        results.dates = dates;
        if(peopleAndRoles != null)
            results.people = peopleAndRoles;
        return results;
    }

    public static void main(String args[])
        throws Throwable
    {
        String sentence = " art director Gene Allen, singer Marni Nixon and restorers Robert A. Harris and James C. Katz; behind - the - scenes documentary 'The fairest fair lady'; alternate Audrey Hepburn vocal versions of 'Show me' and 'Wouldn't it be loverly'; interactive menus; production notes; 4 theatrical trailers; scene access";
        if(args.length > 0)
            sentence = args[0];
        Processor processor = new Processor();
        NlpResults results = processor.process(sentence);
        System.out.println(results.dates);
        System.out.println(results.people);
        processor = null;
        System.gc();
        try
        {
            Thread.sleep(20000L);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public static GenericPipeline pipeline = null;
    public RoleExtraction roleExtractor;

}
