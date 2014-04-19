// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RoleExtraction.java

package org.olac.nlp;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Version;

public class RoleExtraction
{

    public RoleExtraction()
        throws IOException, ParseException
    {
        initIndex();
        roles = getDefaultRoles();
        addToNameIndex(getDefaultNames());
    }

    public RoleExtraction(HashMap roles)
        throws IOException, ParseException
    {
        initIndex();
        this.roles = roles;
        addToNameIndex(getDefaultNames());
    }

    public RoleExtraction(ArrayList names)
        throws IOException, ParseException
    {
        initIndex();
        roles = getDefaultRoles();
        addToNameIndex(names);
    }

    public RoleExtraction(HashMap roles, ArrayList names)
        throws IOException, ParseException
    {
        initIndex();
        this.roles = roles;
        addToNameIndex(names);
    }

    public HashMap extractRoles(ArrayList names, String text)
        throws IOException, ParseException
    {
        HashMap results = new HashMap();
        addToTextIndex(text);
        String name;
        String authorityName;
        for(Iterator iterator = names.iterator(); iterator.hasNext(); results.put(authorityName, processName(name)))
        {
            name = (String)iterator.next();
            authorityName = searchName(name);
        }

        return results;
    }

    private HashSet processName(String name)
        throws CorruptIndexException, IOException, ParseException
    {
        HashSet foundRoles = new HashSet();
        for(Iterator iterator = roles.entrySet().iterator(); iterator.hasNext();)
        {
            java.util.Map.Entry entry = (java.util.Map.Entry)iterator.next();
            String label = (String)entry.getKey();
            for(Iterator iterator1 = ((ArrayList)entry.getValue()).iterator(); iterator1.hasNext();)
            {
                String value = (String)iterator1.next();
                String searchQuery = value.replace("x1", name);
                boolean found = searchText(searchQuery);
                if(found)
                    foundRoles.add(label);
            }

        }

        return foundRoles;
    }

    private boolean searchText(String searchText)
        throws CorruptIndexException, IOException, ParseException
    {
        String queryText = (new StringBuilder("\"")).append(searchText).append("\"~1").toString();
        Query q = (new QueryParser(Version.LUCENE_35, "text", analyzer)).parse(queryText);
        int hitsPerPage = 10;
        IndexReader reader = IndexReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        searcher.search(q, collector);
        ScoreDoc hits[] = collector.topDocs().scoreDocs;
        boolean found = false;
        if(hits.length != 0)
        {
            int score = Math.round(hits[0].score * 10F);
            int docId = hits[0].doc;
            Document d = searcher.doc(docId);
            if(score > 12)
                found = true;
        }
        searcher.close();
        return found;
    }

    private String searchName(String rawName)
        throws ParseException, CorruptIndexException, IOException
    {
        String result = new String();
        Query q = (new QueryParser(Version.LUCENE_35, "person", analyzer)).parse(rawName);
        int hitsPerPage = 10;
        IndexReader reader = IndexReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        searcher.search(q, collector);
        ScoreDoc hits[] = collector.topDocs().scoreDocs;
        if(hits.length != 0)
        {
            int score = Math.round(hits[0].score * 10F);
            int docId2 = hits[0].doc;
            Document d2 = searcher.doc(docId2);
            System.out.println((new StringBuilder("score -->")).append(score).append(" -->").append(rawName).append(" ---> ").append(d2.get("person")).toString());
            if(score > 13)
            {
                int docId = hits[0].doc;
                Document d = searcher.doc(docId);
                result = d.get("person");
            } else
            {
                result = rawName;
            }
        } else
        {
            result = rawName;
        }
        return result;
    }

    private void addToTextIndex(String text)
        throws IOException, ParseException
    {
        cleanIndex("text");
        ArrayList documents = new ArrayList();
        documents.add(text);
        String as[];
        int i1 = (as = text.split(";")).length;
        for(int i = 0; i < i1; i++)
        {
            String t = as[i];
            documents.add(t);
        }

        i1 = (as = text.split(",")).length;
        for(int j = 0; j < i1; j++)
        {
            String t = as[j];
            documents.add(t);
        }

        i1 = (as = text.split("-")).length;
        for(int k = 0; k < i1; k++)
        {
            String t = as[k];
            documents.add(t);
        }

        i1 = (as = text.split(".")).length;
        for(int l = 0; l < i1; l++)
        {
            String t = as[l];
            documents.add(t);
        }

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        IndexWriter w = new IndexWriter(index, config);
        Document doc;
        for(Iterator iterator = documents.iterator(); iterator.hasNext(); w.addDocument(doc))
        {
            String d = (String)iterator.next();
            doc = new Document();
            doc.add(new Field("text", d, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.ANALYZED));
        }

        w.close();
    }

    public void addToNameIndex(ArrayList names)
        throws IOException, ParseException
    {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        IndexWriter writer = new IndexWriter(index, config);
        Document doc;
        for(Iterator iterator = names.iterator(); iterator.hasNext(); writer.addDocument(doc))
        {
            String name = (String)iterator.next();
            doc = new Document();
            doc.add(new Field("person", name, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.ANALYZED));
        }

        writer.close();
    }

    private void initIndex()
        throws IOException, ParseException
    {
        Set stopWords = new HashSet();
        analyzer = new StandardAnalyzer(Version.LUCENE_35, stopWords);
        index = new RAMDirectory();
    }

    private void cleanIndex(String field)
        throws CorruptIndexException, LockObtainFailedException, IOException, ParseException
    {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        IndexWriter writer = new IndexWriter(index, config);
        Query deleteQuery = (new QueryParser(Version.LUCENE_35, field, analyzer)).parse("[a* TO z*]");
        writer.deleteDocuments(deleteQuery);
        writer.close();
    }

    public static HashMap getDefaultRoles()
    {
        HashMap roles = new HashMap();
        roles.put("adaptation", new ArrayList());
        ((ArrayList)roles.get("adaptation")).add("adapted from x1");
        ((ArrayList)roles.get("adaptation")).add("based on x1");
        ((ArrayList)roles.get("adaptation")).add("from the novel x1");
        ((ArrayList)roles.get("adaptation")).add("from the play x1");
        ((ArrayList)roles.get("adaptation")).add("from the story x1");
        roles.put("animator", new ArrayList());
        ((ArrayList)roles.get("animator")).add("x1, animation");
        ((ArrayList)roles.get("animator")).add("animation, x1");
        ((ArrayList)roles.get("animator")).add("animation by x1");
        ((ArrayList)roles.get("animator")).add("x1, animation director");
        ((ArrayList)roles.get("animator")).add("animation director, x1");
        ((ArrayList)roles.get("animator")).add("x1, animator");
        ((ArrayList)roles.get("animator")).add("animator, x1");
        ((ArrayList)roles.get("animator")).add("x1, director of animation");
        ((ArrayList)roles.get("animator")).add("director of animation, x1");
        roles.put("art director", new ArrayList());
        ((ArrayList)roles.get("art director")).add("x1, art director");
        ((ArrayList)roles.get("art director")).add("art director x1");
        ((ArrayList)roles.get("art director")).add("art design, x1");
        ((ArrayList)roles.get("art director")).add("x1, art design ");
        ((ArrayList)roles.get("art director")).add("art design, x1 ");
        ((ArrayList)roles.get("art director")).add("art designed by x1");
        ((ArrayList)roles.get("art director")).add("x1, art designer");
        ((ArrayList)roles.get("art director")).add("art designer, x1");
        roles.put("cinematographer", new ArrayList());
        ((ArrayList)roles.get("cinematographer")).add("x1, camera");
        ((ArrayList)roles.get("cinematographer")).add("camera, x1");
        ((ArrayList)roles.get("cinematographer")).add("x1, cinematographer");
        ((ArrayList)roles.get("cinematographer")).add("cinematographer, x1");
        ((ArrayList)roles.get("cinematographer")).add("x1, cinematography");
        ((ArrayList)roles.get("cinematographer")).add("cinematography,x1");
        ((ArrayList)roles.get("cinematographer")).add("x1, director of photography");
        ((ArrayList)roles.get("cinematographer")).add("director of photography, x1");
        ((ArrayList)roles.get("cinematographer")).add("x1, photographer");
        ((ArrayList)roles.get("cinematographer")).add("photographer, x1");
        ((ArrayList)roles.get("cinematographer")).add("x1, photography");
        ((ArrayList)roles.get("cinematographer")).add("photography, x1");
        ((ArrayList)roles.get("cinematographer")).add("shot by x1");
        roles.put("conductor", new ArrayList());
        ((ArrayList)roles.get("conductor")).add("conducted by x1");
        ((ArrayList)roles.get("conductor")).add("x1, conductor");
        ((ArrayList)roles.get("conductor")).add("conductors, x1");
        roles.put("costume designer", new ArrayList());
        ((ArrayList)roles.get("costume designer")).add("x1, costume design ");
        ((ArrayList)roles.get("costume designer")).add("costume design, x1 ");
        ((ArrayList)roles.get("costume designer")).add("costume design by x1");
        ((ArrayList)roles.get("costume designer")).add("x1, costume designer");
        ((ArrayList)roles.get("costume designer")).add("costume designer, x1");
        ((ArrayList)roles.get("costume designer")).add("costumes designed by x1");
        roles.put("directer", new ArrayList());
        ((ArrayList)roles.get("directer")).add("directed for television by x1");
        ((ArrayList)roles.get("directer")).add("directed for the stage by x1");
        ((ArrayList)roles.get("directer")).add("directed for TV by x1");
        ((ArrayList)roles.get("directer")).add("directed for video by x1");
        roles.put("director", new ArrayList());
        ((ArrayList)roles.get("director")).add("a x1 film");
        ((ArrayList)roles.get("director")).add("a film by x1");
        ((ArrayList)roles.get("director")).add("a picture by x1");
        ((ArrayList)roles.get("director")).add("an x1 film");
        ((ArrayList)roles.get("director")).add("directed by x1");
        ((ArrayList)roles.get("director")).add("x1, direction");
        ((ArrayList)roles.get("director")).add("direction, x1");
        ((ArrayList)roles.get("director")).add("x1, director");
        ((ArrayList)roles.get("director")).add("director, x1");
        ((ArrayList)roles.get("director")).add("x1, film direction");
        ((ArrayList)roles.get("director")).add("film direction, x1");
        ((ArrayList)roles.get("director")).add("film direction by x1");
        ((ArrayList)roles.get("director")).add("x1, film director");
        ((ArrayList)roles.get("director")).add("film director, x1");
        ((ArrayList)roles.get("director")).add("x1, stage direction");
        ((ArrayList)roles.get("director")).add("stage direction, x1");
        ((ArrayList)roles.get("director")).add("stage direction by x1");
        ((ArrayList)roles.get("director")).add("x1, stage director");
        ((ArrayList)roles.get("director")).add("stage director, x1");
        ((ArrayList)roles.get("director")).add("x1, television direction");
        ((ArrayList)roles.get("director")).add("television direction, x1");
        ((ArrayList)roles.get("director")).add("television direction by x1");
        ((ArrayList)roles.get("director")).add("x1, television director");
        ((ArrayList)roles.get("director")).add("television director, x1");
        ((ArrayList)roles.get("director")).add("x1, TV direction");
        ((ArrayList)roles.get("director")).add("TV direction, x1");
        ((ArrayList)roles.get("director")).add("TV direction by x1");
        ((ArrayList)roles.get("director")).add("x1, TV director");
        ((ArrayList)roles.get("director")).add("TV director, x1");
        ((ArrayList)roles.get("director")).add("x1, video direction");
        ((ArrayList)roles.get("director")).add("video direction, x1");
        ((ArrayList)roles.get("director")).add("video direction by x1");
        ((ArrayList)roles.get("director")).add("x1, video director");
        ((ArrayList)roles.get("director")).add("video director, x1");
        roles.put("editor", new ArrayList());
        ((ArrayList)roles.get("editor")).add("edited by x1");
        ((ArrayList)roles.get("editor")).add("x1, editing");
        ((ArrayList)roles.get("editor")).add("editing, x1");
        ((ArrayList)roles.get("editor")).add("editing by x1");
        ((ArrayList)roles.get("editor")).add("x1, editor");
        ((ArrayList)roles.get("editor")).add("editor, x1");
        ((ArrayList)roles.get("editor")).add("film edited by x1");
        ((ArrayList)roles.get("editor")).add("x1, film editing");
        ((ArrayList)roles.get("editor")).add("film editing, x1");
        ((ArrayList)roles.get("editor")).add("film editing by x1");
        ((ArrayList)roles.get("editor")).add("x1, film editor");
        ((ArrayList)roles.get("editor")).add("film editor, x1");
        ((ArrayList)roles.get("editor")).add("video edited by x1");
        ((ArrayList)roles.get("editor")).add("x1, video editing");
        ((ArrayList)roles.get("editor")).add("video editing, x1");
        ((ArrayList)roles.get("editor")).add("video editing by x1");
        ((ArrayList)roles.get("editor")).add("x1, video editor");
        ((ArrayList)roles.get("editor")).add("video editor, x1");
        roles.put("music", new ArrayList());
        ((ArrayList)roles.get("music")).add("composed by x1");
        ((ArrayList)roles.get("music")).add("x1, composer");
        ((ArrayList)roles.get("music")).add("composer, x1");
        ((ArrayList)roles.get("music")).add("x1, music");
        ((ArrayList)roles.get("music")).add("music, x1");
        ((ArrayList)roles.get("music")).add("music by x1");
        ((ArrayList)roles.get("music")).add("music composed by x1");
        ((ArrayList)roles.get("music")).add("x1, music composer");
        ((ArrayList)roles.get("music")).add("music composer, x1");
        ((ArrayList)roles.get("music")).add("original music by x1");
        roles.put("narrator", new ArrayList());
        ((ArrayList)roles.get("narrator")).add("narrated by x1");
        ((ArrayList)roles.get("narrator")).add("x1, narration");
        ((ArrayList)roles.get("narrator")).add("narration, x1");
        ((ArrayList)roles.get("narrator")).add("narration by x1");
        ((ArrayList)roles.get("narrator")).add("x1, narrator");
        ((ArrayList)roles.get("narrator")).add("narrator, x1");
        roles.put("presents", new ArrayList());
        ((ArrayList)roles.get("presents")).add("x1 present");
        ((ArrayList)roles.get("presents")).add("presented by x1");
        ((ArrayList)roles.get("presents")).add("x1 presents");
        roles.put("producer", new ArrayList());
        ((ArrayList)roles.get("producer")).add("a x1 coproduction");
        ((ArrayList)roles.get("producer")).add("a x1 co-production");
        ((ArrayList)roles.get("producer")).add("a x1 production");
        ((ArrayList)roles.get("producer")).add("a coproduction of x1");
        ((ArrayList)roles.get("producer")).add("a co-production of x1");
        ((ArrayList)roles.get("producer")).add("a production by x1");
        ((ArrayList)roles.get("producer")).add("an x1 coproduction");
        ((ArrayList)roles.get("producer")).add("an x1 co-production");
        ((ArrayList)roles.get("producer")).add("an x1 production");
        ((ArrayList)roles.get("producer")).add("x1, coproducer");
        ((ArrayList)roles.get("producer")).add("coproducer, x1");
        ((ArrayList)roles.get("producer")).add("x1, co-producer");
        ((ArrayList)roles.get("producer")).add("co-producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, executive producer");
        ((ArrayList)roles.get("producer")).add("executive producer, x1");
        ((ArrayList)roles.get("producer")).add("produced by x1");
        ((ArrayList)roles.get("producer")).add("produced for television by x1");
        ((ArrayList)roles.get("producer")).add("produced for the stage by x1");
        ((ArrayList)roles.get("producer")).add("produced for TV by x1");
        ((ArrayList)roles.get("producer")).add("produced for video by x1");
        ((ArrayList)roles.get("producer")).add("x1, producer");
        ((ArrayList)roles.get("producer")).add("producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, production");
        ((ArrayList)roles.get("producer")).add("production, x1");
        ((ArrayList)roles.get("producer")).add("x1, senior producer");
        ((ArrayList)roles.get("producer")).add("senior producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, series producer");
        ((ArrayList)roles.get("producer")).add("series producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, producer");
        ((ArrayList)roles.get("producer")).add("producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, production");
        ((ArrayList)roles.get("producer")).add("production, x1");
        ((ArrayList)roles.get("producer")).add("stage production by x1");
        ((ArrayList)roles.get("producer")).add("x1, producer");
        ((ArrayList)roles.get("producer")).add("producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, production");
        ((ArrayList)roles.get("producer")).add("production, x1");
        ((ArrayList)roles.get("producer")).add("television production by x1");
        ((ArrayList)roles.get("producer")).add("x1, producer");
        ((ArrayList)roles.get("producer")).add("producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, production");
        ((ArrayList)roles.get("producer")).add("production, x1");
        ((ArrayList)roles.get("producer")).add("TV production by x1");
        ((ArrayList)roles.get("producer")).add("x1, producer");
        ((ArrayList)roles.get("producer")).add("producer, x1");
        ((ArrayList)roles.get("producer")).add("x1, production");
        ((ArrayList)roles.get("producer")).add("production, x1");
        ((ArrayList)roles.get("producer")).add("video production by x1");
        roles.put("production designer", new ArrayList());
        ((ArrayList)roles.get("production designer")).add("x1, production design ");
        ((ArrayList)roles.get("production designer")).add("production design, x1 ");
        ((ArrayList)roles.get("production designer")).add("production designed by x1");
        ((ArrayList)roles.get("production designer")).add("x1, production designer");
        ((ArrayList)roles.get("production designer")).add("production designer, x1");
        roles.put("set decorator", new ArrayList());
        ((ArrayList)roles.get("set decorator")).add("x1, set decoration");
        ((ArrayList)roles.get("set decorator")).add("set decoration, x1 ");
        ((ArrayList)roles.get("set decorator")).add("set decorated by x1");
        ((ArrayList)roles.get("set decorator")).add("x1, set decorator");
        ((ArrayList)roles.get("set decorator")).add("set decorator, x1");
        roles.put("writer", new ArrayList());
        ((ArrayList)roles.get("writer")).add("x1, dialog");
        ((ArrayList)roles.get("writer")).add("dialog, x1");
        ((ArrayList)roles.get("writer")).add("dialog by x1");
        ((ArrayList)roles.get("writer")).add("x1, dialogue");
        ((ArrayList)roles.get("writer")).add("dialogue, x1");
        ((ArrayList)roles.get("writer")).add("dialogue by x1");
        ((ArrayList)roles.get("writer")).add("x1, screen play");
        ((ArrayList)roles.get("writer")).add("screen play, x1");
        ((ArrayList)roles.get("writer")).add("screen play by x1");
        ((ArrayList)roles.get("writer")).add("x1, screenplay");
        ((ArrayList)roles.get("writer")).add("screenplay, x1");
        ((ArrayList)roles.get("writer")).add("screenplay by x1");
        ((ArrayList)roles.get("writer")).add("x1, script");
        ((ArrayList)roles.get("writer")).add("script, x1");
        ((ArrayList)roles.get("writer")).add("script by x1");
        ((ArrayList)roles.get("writer")).add("x1, script writer");
        ((ArrayList)roles.get("writer")).add("script writer, x1");
        ((ArrayList)roles.get("writer")).add("x1, scriptwriter");
        ((ArrayList)roles.get("writer")).add("scriptwriter, x1");
        ((ArrayList)roles.get("writer")).add("x1, writer");
        ((ArrayList)roles.get("writer")).add("writer, x1");
        ((ArrayList)roles.get("writer")).add("written by x1");
        return roles;
    }

    public static ArrayList getDefaultNames()
    {
        ArrayList names = new ArrayList();
        names.add("Spielberg, Steven");
        return names;
    }

    public static void main(String args[])
        throws IOException, ParseException
    {
        String text = "screenplay and direction, Sergei Eisenstein and Grigori ";
        ArrayList names = new ArrayList();
        names.add("Steven Spielberg");
        names.add("Chris Fitzpatrick");
        names.add("Sergei Eisenstein");
        RoleExtraction re = new RoleExtraction();
        HashMap results = re.extractRoles(names, text);
        System.out.print(results);
        String text2 = "blah blah production, Chris Fitzpatrick blah blah";
        HashMap results2 = re.extractRoles(names, text2);
        System.out.print(results2);
        String text3 = "fooo fooo fooo fooo Chris Fitzpatrick fooo fooo foo";
        HashMap results3 = re.extractRoles(names, text3);
        System.out.print(results3);
    }

    public HashMap roles;
    public StandardAnalyzer analyzer;
    public Directory index;
}
