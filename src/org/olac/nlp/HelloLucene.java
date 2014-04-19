// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   HelloLucene.java

package org.olac.nlp;

import java.io.IOException;
import java.io.PrintStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class HelloLucene
{

    public HelloLucene()
    {
    }

    public static void main(String args[])
        throws IOException, ParseException
    {
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        IndexWriter w = new IndexWriter(index, config);
        addDoc(w, "Spielberg, Steven");
        addDoc(w, "Written By ");
        addDoc(w, "Lucene for Dummies");
        addDoc(w, "Managing Gigabytes");
        addDoc(w, "directed by Charlotte Zwerin ; producers, Charlotte Zwerin, Bruce Ricker ; executive producer, Clint Eastwood");
        w.close();
        IndexWriterConfig config2 = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        IndexWriter w2 = new IndexWriter(index, config2);
        Query q1 = (new QueryParser(Version.LUCENE_35, "title", analyzer)).parse("title:[a* TO z*]");
        w2.deleteDocuments(q1);
        w2.deleteDocuments(q1);
        w2.close();
        String querystr = args.length <= 0 ? "Steven Spielberg" : args[0];
        Query q = (new QueryParser(Version.LUCENE_35, "title", analyzer)).parse(querystr);
        int hitsPerPage = 10;
        IndexReader reader = IndexReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        searcher.search(q, collector);
        ScoreDoc hits[] = collector.topDocs().scoreDocs;
        System.out.println((new StringBuilder("Found ")).append(hits.length).append(" hits.").toString());
        for(int i = 0; i < hits.length; i++)
        {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((new StringBuilder(String.valueOf(i + 1))).append(". ").append(d.get("title")).toString());
        }

        searcher.close();
    }

    private static void addDoc(IndexWriter w, String value)
        throws IOException
    {
        Document doc = new Document();
        doc.add(new Field("title", value, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.ANALYZED));
        w.addDocument(doc);
    }
}
