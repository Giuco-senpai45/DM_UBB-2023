package org.example;
import org.apache.lucene.document.Document;

/**
 * This class is a wrapper over the simple Document class.
 * We additionally store the score of the document for further usage in evaluating the performance
 */
public class ResultClass {
    Document DocName;
    double docScore = 0;
}