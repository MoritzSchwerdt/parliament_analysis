package BackendHelpers;

import com.mongodb.AggregationOptions;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UnwindOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import spark.QueryParamsMap;

import static BackendHelpers.AggregationHelper.*;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;

import com.mongodb.AggregationOptions.Builder;

import java.util.*;


import java.util.Arrays;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.conversions.Bson;
import java.util.concurrent.TimeUnit;
import org.bson.Document;


public class AggregationBuilder {

    public List<Bson> createTokenAggregation(QueryParamsMap queryParams){
        //todo: check if input params are valid
        return Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                lookupHelper("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analyzed"),
                lookupHelper("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                matchHelper("redner._id",queryParams.get("rednerID").value()),
                matchHelper("redner.fraktion",queryParams.get("fraktion").value()),
                matchHelper("redner.party",queryParams.get("party").value()),
                replaceDateStringByDate("date"),
                createMatchByDate("$date",queryParams.get("startDate").value(),queryParams.get("endDate").value()),
                unwindHelper("$analyzed"),
                new Document("$replaceRoot",
                        new Document("newRoot", "$analyzed")),
                unwindHelper("$token"),
                new Document("$group",
                        new Document("_id", "$token")
                                .append("count",
                                        new Document("$sum", 1))),
                new Document("$sort",
                        new Document("count", -1)),
                new Document("$match",
                        new Document("count",
                                new Document("$gte", 99))));


    }
    public List<Bson> createNamedEntitiesAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                new Document("$facet",
                        new Document("persons", Arrays.asList(
                                unwindHelper("$persons"),
                                new Document("$group",
                                            new Document("_id", "$persons")
                                            .append("count",
                                                    new Document("$sum",1))),
                                new Document("$project",
                                        new Document("count", 1)
                                                .append("element", "$_id")
                                                .append("_id", 0)),
                                new Document("$sort",
                                        new Document("count", -1))))
                        .append("organisations",Arrays.asList(
                                unwindHelper("$organisations"),
                                new Document("$group",
                                        new Document("_id", "$organisations")
                                                .append("count",
                                                        new Document("$sum",1))),
                                new Document("$project",
                                        new Document("count", 1)
                                                .append("element", "$_id")
                                                .append("_id", 0)),
                                new Document("$sort",
                                        new Document("count", -1))))
                        .append("locations",Arrays.asList(
                                unwindHelper("$locations"),
                                new Document("$group",
                                        new Document("_id", "$locations")
                                                .append("count",
                                                        new Document("$sum",1))),
                                new Document("$project",
                                        new Document("count", 1)
                                                .append("element", "$_id")
                                                .append("_id", 0)),
                                new Document("$sort",
                                        new Document("count", -1)))))
        );
    }
    public List<Bson> createSpeechAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                matchHelper("tagesordnungspunkte.reden.redeID", queryParams.get("redeID").value()),
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                new Document("$project",
                        new Document("date", "$date")
                                .append("speaker", "$tagesordnungspunkte.reden.rednerID")
                                .append("id", "$tagesordnungspunkte.reden.redeID")
                                .append("content", "$tagesordnungspunkte.reden.content")
                                .append("_id",0)
                                .append("perodeID","$_id")),
                new Document("$set",
                        new Document("sucess", true)));
    }
    public List<Bson> createSpeakersAggregation(QueryParamsMap queryParams){
        return Arrays.asList(speakerMatchHelper("party",queryParams),
                matchHelper("fraktion", queryParams.get("fraktion").value()),
                matchHelper("_id", queryParams.get("speakerID").value()));
    }

    public Document testAggregation(){
        /*
         * Requires the MongoDB Java Driver.
         * https://mongodb.github.io/mongo-java-driver
         */

        MongoClient mongoClient = new MongoClient(
                new MongoClientURI(
                        "mongodb://PRG_WiSe21_Gruppe_1_3:aNx8P12u@prg2021.texttechnologylab.org:27020/?authSource=PRG_WiSe21_Gruppe_1_3&readPreference=primary&appname=MongoDB+Compass&directConnection=true&ssl=false"
                )
        );
        MongoDatabase database = mongoClient.getDatabase("PRG_WiSe21_Gruppe_1_3");
        MongoCollection<Document> collection = database.getCollection("speeches");

        List<Document> result = collection.aggregate(Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                lookupHelper("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analyzed"),
                lookupHelper("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                matchHelper("redner._id",null),
                matchHelper("redner.fraktion",null),
                matchHelper("redner.party",null),
                replaceDateStringByDate("date"),
                createMatchByDate("$date",null,null),
                unwindHelper("$analyzed"),
                new Document("$replaceRoot",
                        new Document("newRoot", "$analyzed")),
                unwindHelper("$token"),
                new Document("$group",
                        new Document("_id", "$token")
                                .append("count",
                                        new Document("$sum", 1))),
                new Document("$sort",
                        new Document("count", -1)),
                new Document("$match",
                        new Document("count",
                                new Document("$gte", 99))))).into(new ArrayList<>());
        //System.out.println(result.toString());
        return result.get(0);
    }



}

/*
unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                lookupHelper("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analyzed"),
                lookupHelper("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                matchHelper("redner._id",queryParams.get("rednerID").value()),
                matchHelper("redner.fraktion",queryParams.get("fraktion").value()),
                matchHelper("redner.party",queryParams.get("party").value()),
                unwindHelper("$analyzed"),
                new Document("$replaceRoot",
                        new Document("newRoot", "$analyzed")),
                unwindHelper("$token"),
                new Document("$group",
                        new Document("_id", "$token")
                                .append("count",
                                        new Document("$sum", 1L))),
                new Document("$project",
                        new Document("_id", 0L)
                                .append("token", "$_id")
                                .append("count",
                                        new Document("$filter",
                                                new Document("input", "$count")
                                                        .append("as", "count")
                                                        .append("cond",
                                                                new Document("$gte", Arrays.asList("$$count",min)))))),
                new Document("$sort",
                        new Document("count", -1L)));

                        public List<Bson> createTokenAggregation(QueryParamsMap queryParams){
        Integer min = 0;
        UnwindOptions unwindOptions = new UnwindOptions();
        unwindOptions.preserveNullAndEmptyArrays(true);
        try {
            min = queryParams.get("minimum") == null ? 0 : Integer.parseInt(queryParams.get("minimum").value());
        }catch (NumberFormatException e){
        }
        return Arrays.asList(
                Aggregates.unwind("$tagesordnungspunkte", unwindOptions),
                Aggregates.unwind("$tagesordnungspunkte.reden", unwindOptions),
                Aggregates.lookup("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analysiert"),
                Aggregates.lookup("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                Aggregates.match(matchHelper("$redner._id",queryParams.get("rednerID").value())),
                Aggregates.match(matchHelper("$redner.fraktion",queryParams.get("fraktion").value())),
                Aggregates.match(matchHelper("$redner.party",queryParams.get("party").value())),
                Aggregates.unwind("$analysiert", unwindOptions),
                Aggregates.replaceRoot("$analysiert"),
                Aggregates.unwind("$token", unwindOptions),
                Aggregates.limit(10000),
                Aggregates.group("$token", sum("count",1)));


    }
    public List<Bson> createGroupAggregation(QueryParamsMap queryParams){
        Integer min = 0;
        UnwindOptions unwindOptions = new UnwindOptions();
        unwindOptions.preserveNullAndEmptyArrays(true);
        return Arrays.asList(
                Aggregates.unwind("$tagesordnungspunkte", unwindOptions),
                Aggregates.unwind("$tagesordnungspunkte.reden", unwindOptions),
                Aggregates.lookup("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analysiert"),
                Aggregates.lookup("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                Aggregates.group("$periode", sum("count",1L)));


    }

    public List<Bson> WAS TOO SLOW, DONT USe (QueryParamsMap queryParams){
        Integer min = 0;
        UnwindOptions unwindOptions = new UnwindOptions();
        unwindOptions.preserveNullAndEmptyArrays(true);
        try {
            min = queryParams.get("minimum") == null ? 0 : Integer.parseInt(queryParams.get("minimum").value());
        }catch (NumberFormatException e){
        }
        return Arrays.asList(
                Aggregates.unwind("$tagesordnungspunkte", unwindOptions),
                Aggregates.unwind("$tagesordnungspunkte.reden", unwindOptions),
                Aggregates.lookup("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analysiert"),
                Aggregates.lookup("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                Aggregates.match(matchHelper("$redner._id",queryParams.get("rednerID").value())),
                Aggregates.match(matchHelper("$redner.fraktion",queryParams.get("fraktion").value())),
                Aggregates.match(matchHelper("$redner.party",queryParams.get("party").value())),
                Aggregates.unwind("$analysiert", unwindOptions),
                Aggregates.replaceRoot("$analysiert"),
                Aggregates.unwind("$token", unwindOptions),
                Aggregates.limit(10000),
                Aggregates.group("$token", sum("count",1)));


    }
 */
