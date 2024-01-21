package org.example;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class QueryEngine {
    String inputFilePath;
    private final static String INDEX_PATH = "src\\main\\resources\\index.lucene";
    boolean indexExists;
    StandardAnalyzer analyzer = new StandardAnalyzer();
    FSDirectory index;

    /**
    This constructor checks if the index for the wiki pages is created.

    If it's not, it builds the index, otherwise it sets the parameter index to the path to the existing index.
    @param inputFile
     **/
    public QueryEngine(String inputFile) {
        this.inputFilePath = inputFile;

        // Check if the file index exists and build the index if it doesn't
        File file = new File(INDEX_PATH);
        if (file.exists()) {
            try {
                this.index = FSDirectory.open(Paths.get(INDEX_PATH));
            } catch (IOException e) {
                System.err.println("==QueryEngine(String inputFile): The file for the index directory wasn't able to be opened or created || " + e);
                System.exit(1);
            }
            this.indexExists = true;
        } else {
            this.indexExists = false;
            try {
                buildIndex();
            } catch (IOException e) {
                System.err.println("==QueryEngine(String inputFile): The file for the index directory wasn't able to be opened or created || " + e);
                System.exit(1);
            }
        }
    }

    /**
    Creates the actual index by going to through files present in the inputFilePath directory.
     **/
    private void buildIndex() throws IOException {
        // Get input file(s) from resources folder
        File[] files = { new File(this.inputFilePath) };
        if (files[0].isDirectory()) {
            files = files[0].listFiles();
        }

        // Check if we got any files in the file array
        assert files != null;
        // Retrieve the document(s) in each file
        for (File value : files) {
            // Contents of the file
            String file = Files.readString(Paths.get(value.toURI()));

            // Generate a writer to add documents to the index
            this.index = FSDirectory.open(Paths.get(INDEX_PATH));
            IndexWriter w = new IndexWriter(this.index, new IndexWriterConfig(this.analyzer));
            int contentEnd = 0;
            int contentStart;
            while (contentEnd + 2 < file.length()) {
                // Assign the start location for the content of the next document
                contentStart = file.indexOf("]]\n\n", contentEnd) + 4;

                // Get the document ID from lines that respect the pattern: [[DocTitle]]
                String docName;
                if (contentEnd == 0) {
                    docName = file.substring(contentEnd + 2, contentStart - 4);
                } else {
                    docName = file.substring(contentEnd + 5, contentStart - 4);
                }

                // Fill a string with the actual contents of the document
                // Need to check out for the [[File: and [[Image: strings
                // which do not mark the beginning of a new document. We incrementally increase the content each time
                // by 10 characters at a time
                int offset = 0;
                while ((contentEnd = file.indexOf("\n\n\n[[", contentStart + offset))
                        ==
                        file.indexOf("\n\n\n[[File:", contentStart + offset)
                        ||
                        (contentEnd = file.indexOf("\n\n\n[[", contentStart + offset))
                                ==
                                file.indexOf("\n\n\n[[Image:", contentStart + offset)) {
                    if (contentEnd == -1) {
                        contentEnd = file.length() - 1;
                        break;
                    } else {
                        offset += 10;
                    }
                }

                // Derive the contents based on the boundaries evaluated
                String contents = file.substring(contentStart, contentEnd);

                // Lemmatize and setup the document for indexing
                Properties props = new Properties();
                props.setProperty("annotators", "tokenize,ssplit,pos,lemma"); // Lemmatize the content of the document
                StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
                CoreDocument document = new CoreDocument(contents);
                pipeline.annotate(document);

                // Generate an index for the document and it's contents
                addDoc(w, docName, document);
            }
            // Close the index writer and file scanner objects
            w.close();
        }
        this.indexExists = true;
    }

    /**
    Creates a Document class that to be added to the lucene index.
    The created document will contain two fields:
    - docid, with the title of the document
    - content, with the actual content of the document
    @param w, the indexWriter that will add documents to the index
    @param docId, the title of the document, we will refer to this as the document id
    @param passedDoc, the content of the document
     **/
    private void addDoc(IndexWriter w, String docId, CoreDocument passedDoc) throws IOException {
        // Generate a new document with the fields
        Document document = new Document();
        document.add(new StringField("docid", docId, Field.Store.YES)); // We use StringField for fields that shouldn't be tokenized
        document.add(new TextField("content", passedDoc.text(), Field.Store.YES)); // We use TextField for the content we want to be tokenized

        w.addDocument(document);
    }

    // Purpose: Query a given parsing string across the current index

    /**
     * Parse a given query into a proper format and search the current index to return the top "docCount" documents
     * that respect the query.
     * @param docCount, this many documents will be returned as top N resulted documetns
     * @param query, the string containing the actual query
     * @param k1, saturated term frequency
     * @param b, weight for discriminative terms
     * @return a list of top N relevant documents resulted from the query
     */
    public List<ResultClass> query(int docCount, String query,float k1,float b) {
        ArrayList<ResultClass> result = new ArrayList<ResultClass>();

        try {
            // Lemmatize the query
            Sentence qPassed = new Sentence(query);
            query = qPassed.lemmas().toString().replaceAll("\\p{Punct}", ""); // remove all punctuation signs

            // Stem the query //Stemmer
//                 Stemmer s = new Stemmer();
//                 for (String word : query.split("\\s+")) {
//                 query += s.stem(word) + " ";
//                 }
//                 query = query.trim();


            // Generate a query to navigate given the passed query string
            Query q = new QueryParser("content", this.analyzer).parse(query);

            // Instantiate the necessary reader and searcher methods for query
            IndexReader reader = DirectoryReader.open(this.index);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity(k1, b)); // Set at Best Matching 25 similarity, it's an extension of the classic TF-IDF
//                searcher.setSimilarity(new ClassicSimilarity()); //TF-IDF search method

            // Retrieve the query results
            TopDocs docs = searcher.search(q, docCount);
            ScoreDoc[] hits = docs.scoreDocs;

            // Iterate over all documents returned from the query
            for (int i = 0; i < hits.length; i++) {
                // Add the document to the result list
                ResultClass temp = new ResultClass();
                temp.DocName = searcher.doc(hits[i].doc);
                temp.docScore = hits[i].score;
                result.add(temp);
            }
        } catch (ParseException | IOException e) {
            System.err.println("query(): Failure to parse query - " + query);
        }

        return result;
    }

    // Purpose: Establish an WatsonQueryEngine object that indexes the wiki-data and
    // run a known set of queries to measure performance
    public static void main(String[] args) {
        try {
            String fileName = "src/main/resources/wiki-data";
            System.out.println("Start IBM Watson");
            QueryEngine queryEngine = new QueryEngine(fileName);

            // Run queries with known answers and evaluate the correct returns
            fileName = "src\\main\\resources\\questions.txt";
            float b = 0.5f; // the values for k1 and b were found through an iterative generation of incremental values.
            float k1 = 0.8f;  // this combination yielded the most consecutive high scores of correct answers
            int docCount = 10;
            int correct = 0;
            PerformanceMetrics metrics = new PerformanceMetrics();
            int noOfRelevantItemsAtPos1 = 0;
            double DCG = 0.0, IDCG = 0.0, NDCG = 0.0;
            double MRRSum = 0.0;
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            for (int i = 0; i < lines.size(); i += 4) {
                String category = lines.get(i);
                String question = lines.get(i + 1);
                String answer = lines.get(i + 2);

                List<ResultClass> result = queryEngine.query(docCount, category.trim() + " " + question, k1, b);
                boolean first = false;
                for(int j = 0 ; j < result.size(); j++) {
                    String normalisedRes = result.get(j).DocName.get("docid").toLowerCase();
                    if (answer.toLowerCase().contains(normalisedRes)) {
                        correct++;
                        if(j == 0){
                            noOfRelevantItemsAtPos1++;
                        }
                        if(!first) {
                            MRRSum += 1.0 / (j + 1);
                            first = true;
                        }
                    }
                }
                NDCG += metrics.computeNDCG(result, DCG, IDCG);
            }
            metrics.setMeanReciprocalRank(1.0 / 100.0 * MRRSum);
            metrics.setPrecisionAt1(noOfRelevantItemsAtPos1 / 100.0);
            metrics.setNDCG(NDCG);
            System.out.println(String.format("Relevant ranked pages:%d \nCorrect Answers: %d\nPossible Answers: %d\n", docCount, correct, (lines.size() / 4 + 1)));
            System.out.println("Performance Metrics");
            System.out.println("P@1 = " + metrics.getPrecisionAt1());
            System.out.println("NDCG = " + metrics.getNDCG());
            System.out.println("MRR = " + metrics.getMeanReciprocalRank());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
}