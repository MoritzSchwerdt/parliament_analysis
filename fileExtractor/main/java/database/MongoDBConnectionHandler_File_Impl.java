package database;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import interfaces.MongoDBConnectionHandler;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MongoDBConnectionHandler_File_Impl implements MongoDBConnectionHandler {
    public final MongoDatabase db;
    private final Creds_File_Impl cred;


    public MongoDBConnectionHandler_File_Impl() {
        /**
         * establishes a connection with the mongodb
         */
        this.cred = new Creds_File_Impl();

        // set up mongo db client
        MongoCredential credential = MongoCredential.createScramSha1Credential(cred.getUser(), cred.getDatabase(), cred.getPassword());
        ServerAddress seed = new ServerAddress(cred.getHost(), Integer.parseInt(cred.getPort()));
        List<ServerAddress> seeds = new ArrayList(0);
        seeds.add(seed);
        MongoClientOptions options = MongoClientOptions.builder()
                .connectionsPerHost(50)
                .sslEnabled(false)
                .build();
        MongoClient client = new MongoClient(seeds, credential, options);
        this.db = client.getDatabase(cred.getDatabase());
    }

    public boolean existsDocument(String identifier, String collection){
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", identifier);

        return this.db.getCollection(collection).find(whereQuery).first() != null;
    }
    public Document getDocument(String identifier, String collection){
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", identifier);
        try {
            Document document = this.db.getCollection(collection).find(whereQuery).first();
            return document;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return new Document();
    }


    public boolean updateDocument(Document doc, String collection) throws UIMAException {

        // Create Where-Query
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("_id", doc.get("_id"));
        UpdateResult uResult = null;
        try {
            uResult = this.db.getCollection(collection).replaceOne(whereQuery, doc);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return uResult != null;

    }
    public void uploadBson(String bSon, String collection){
        /**
         * uploads a given bson string to a given collection
          */
        // validate collection name
        if(collection == "protocol"){
            collection = this.cred.getProtocollCollection();
        } else if(collection == "jcas"){
            collection = this.cred.getJcasCollection();
        } else {
            System.out.println("ERROR - " + collection + " is not a valid collection");}
        // parse bson to mongo document
        Document dbDoc = null;
        try {
            dbDoc = Document.parse(bSon);
        } catch (Exception e) {
            System.out.println("The given Bson string is not parseable");
            e.printStackTrace();
        }
        // upload document
        assert dbDoc != null;
        this.db.getCollection(collection).insertOne(dbDoc);
    }
    public void uploadDoc(Document dbDoc, String collection){
        /**
         * uploads a given dbDoc to a given collection
         */
        // validate collection name
        if(collection == "protocol"){
            collection = this.cred.getProtocollCollection();
        } else if(collection == "jcas"){
            collection = this.cred.getJcasCollection();
        } else {
            //System.out.println("ERROR - " + collection + " is not a valid collection");
        }
        // upload document
        assert dbDoc != null;
        if(existsDocument(dbDoc.get("_id").toString(), collection)){return;}
        this.db.getCollection(collection).insertOne(dbDoc);
    }
    public void uploadDocs(List<Document> dbDocs, String collection){
        assert dbDocs != null;
        this.db.getCollection(collection).insertMany(dbDocs);
    }
    public List<Document> aggregateMongo (String collection, List<Bson> aggregation){
        try {
            //mongoTemplate.aggregate(new Aggregation().withOptions(AggregationOptions.Builder.allowDiskUse(true)), "match", Document.class);

            return this.db.getCollection(collection).aggregate(aggregation).allowDiskUse(true).into(new ArrayList<>());
        }catch (MongoException e){
            e.printStackTrace();
            return this.db.getCollection(collection).aggregate(Arrays.asList()).into(new ArrayList<>());
        }
    }



    public String JCasToXML(JCas jcas) throws IOException {
        /**
         * Serialize JCas Object to a xml String
         */
        String outString = "";
        CAS cas = jcas.getCas();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XCASSerializer.serialize(cas, out);
            outString = out.toString();
        } catch (IOException | SAXException ignored) {
        }
        out.close();
        return outString;
    }

    public JCas XMLToJcas(String xml) throws UIMAException {
        /**
         * Deserialize xml String to JCas object
         */
        JCas emptyJcas = JCasFactory.createText("", "de");
        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
        CAS cas = emptyJcas.getCas();
        try {
            XCASDeserializer.deserialize(in, cas);
        } catch (IOException | SAXException e) {
        }
        return cas.getJCas();
    }


    public boolean jCasExists(String redeID) {
        /**
         * checks if jcas obj exists in database
         */
        Document doc = new Document("_id", redeID);
        Document d = db.getCollection(cred.getJcasCollection()).find(doc).first();
        return d != null;
    }

    public boolean protokollExists(String sitzungsnr){
        /**
         * checks if protkoll obj exists in database
         */
        try{

            Document doc = new Document("_id", sitzungsnr);
            Document d = db.getCollection(cred.getProtocollCollection()).find(doc).first();
            return d != null;
        }catch (MongoSocketOpenException e){
            System.out.println("couldnt reach mongo");
        }
        return true;
    }

    public void createPlaceholder(String sitzungsnr){
        /**
         * creates a placeholder for a protkoll in the db so no other instance of the uploader trys to upload it (so no doubling)
         */
        Document doc = new Document("_id", sitzungsnr);
        doc.append("placeholder", true);
        db.getCollection(cred.getProtocollCollection()).insertOne(doc);
        System.out.println("The Placeholder for protkoll " + sitzungsnr + " was created");
    }

    public void removePlaceholder(String sitzungsnr){
        /**
         * removes previously created placeholder
         */
        Document doc = new Document("_id", sitzungsnr);
        doc.append("placeholder", true);
        Document filter = db.getCollection(cred.getProtocollCollection()).find(doc).first();
        if(filter != null) {
            DeleteResult result = db.getCollection(cred.getProtocollCollection()).deleteOne(filter);
            System.out.println("The Placeholder for protkoll " + sitzungsnr + " was deleted");
        } else {
            System.out.println("No Placeholder for protkoll " + sitzungsnr + " was found, therefore it couldn't be deleted");
        }
    }


    public List<String> getExistingPlaceholderIDs(){
        /**
         * checks if protkoll obj exists in database
         */
        LinkedList<String> pIDs = new LinkedList<>();
        Document doc = new Document("placeholder", true);
        FindIterable<Document> placeholders = this.db.getCollection(cred.getProtocollCollection()).find(doc);

        for(Document placeholder : placeholders){
            pIDs.push(placeholder.getString("_id"));
        }
        return pIDs;
    }

    public long countProtokolle(){
        /**
         * counts the amount of protkolls in the db
         */
        return db.getCollection(cred.getProtocollCollection()).countDocuments();
    }


    public JCasTuple_FIle_Impl getRedeJcas(String redeID) throws UIMAException {
        /**
         * creates a JCasTuple (including the reden and comments) from the mongodb
         */
        Document doc = new Document("_id", redeID);
        Document result = db.getCollection(cred.getJcasCollection()).find(doc).first();
        assert result != null;
        String contentXMl = result.getString("contentXML");
        String commentXML = result.getString("commentXML");
        return new JCasTuple_FIle_Impl(XMLToJcas(contentXMl), XMLToJcas(commentXML));
    }


}