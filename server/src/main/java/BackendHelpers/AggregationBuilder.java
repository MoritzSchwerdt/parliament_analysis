package BackendHelpers;

import com.mongodb.AggregationOptions;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UnwindOptions;
import org.bson.BsonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import se.lth.cs.srl.languages.Language;
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

import javax.management.Query;


public class AggregationBuilder {

    /**
     *Methode to create mongo aggregation of token route
     * @param queryParams
     * @return
     */
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
                                new Document("$gte", minimumOfZero(queryParams,"minimum")))));


    }

    /**
     *Methode to create mongo aggregation of named entities route
     * @param queryParams
     * @return
     */
    public List<Bson> createNamedEntitiesAggregation(QueryParamsMap queryParams){
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
                                        new Document("count", -1)),
                                new Document("$match",
                                        new Document("count",
                                                new Document("$gte", minimumOfZero(queryParams,"minimum"))))))
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
                                new Document("$match",
                                        new Document("count",
                                                new Document("$gte", minimumOfZero(queryParams,"minimum")))),
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
                                new Document("$match",
                                        new Document("count",
                                                new Document("$gte", minimumOfZero(queryParams,"minimum")))),
                                new Document("$sort",
                                        new Document("count", -1)))))
        );

    }

    /**
     *Methode to create mongo aggregation of speech route
     * @param queryParams
     * @return
     */
    public List<Bson> createSpeechAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                matchHelper("tagesordnungspunkte.reden.redeID", queryParams.get("redeID").value()),
                new Document("$project",
                        new Document("date", "$date")
                                .append("speaker", "$tagesordnungspunkte.reden.rednerID")
                                .append("id", "$tagesordnungspunkte.reden.redeID")
                                .append("content", "$tagesordnungspunkte.reden.content")
                                .append("_id",0)
                                .append("perodeID","$_id")),
                new Document("$lookup",
                        new Document("from", "NotSoanalyzedSpeeches")
                                .append("localField", "id")
                                .append("foreignField", "_id")
                                .append("as", "analyzed")),
                unwindHelper("$analyzed"));
    }

    /**
     *Methode to create mongo aggregation of speakers route
     * @param queryParams
     * @return
     */
    public List<Bson> createSpeakersAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                partyMatchHelper("party",queryParams),
                matchHelper("fraktion", queryParams.get("fraktion").value()),
                matchHelper("_id", queryParams.get("rednerID").value()),
                new Document("$project",
                        new Document("firstname", "$firstname")
                                .append("role", "rolle")
                                .append("name", "$lastname")
                                .append("fraction", "$fraktion")
                                .append("party", "$party")
                                .append("picture", "$picture")
                                .append("_id", 0)));
    }

    /**
     *Methode to create mongo aggregation of sentiment route
     * @param queryParams
     * @return
     */
    public List<Bson> createSentimentAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                replaceDateStringByDate("date"),
                createMatchByDate("$date",queryParams.get("startDate").value(),queryParams.get("endDate").value()),
                lookupHelper("analyzedSpeeches","tagesordnungspunkte.reden.redeID","_id","analyzed"),
                lookupHelper("speakers","tagesordnungspunkte.reden.rednerID","_id","redner"),
                matchHelper("tagesordnungspunkte.reden.rednerID", queryParams.get("rednerID").value()),
                matchHelper("redner.fraktion", queryParams.get("fraktion").value()),
                partyMatchHelper("party",queryParams),
                unwindHelper("$analyzed"),
                new Document("$replaceRoot",
                        new Document("newRoot", "$analyzed")),
                new Document("$group",
                        new Document("_id", "$sentiment")
                                .append("count",
                                        new Document("$sum", 1))),
                new Document("$project",
                        new Document("_id", 0)
                                .append("sentiment", "$_id")
                                .append("count", 1)),
                new Document("$sort",
                        new Document("count", -1)));
    }

    /**
     *Methode to create mongo aggregation of parties route
     * @param queryParams
     * @return
     */
    public List<Bson> createPartiesAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                replaceDateStringByDate("date"),
                createMatchByDate("$date",queryParams.get("startDate").value(),queryParams.get("endDate").value()),
                new Document("$lookup",
                        new Document("from", "speakers")
                                .append("localField", "tagesordnungspunkte.reden.rednerID")
                                .append("foreignField", "_id")
                                .append("as", "redner")),
                unwindHelper("$redner"),
                matchHelper("redner.party", null),
                new Document("$group",
                        new Document("_id", "$redner.party")
                                .append("count",
                                        new Document("$sum", 1L))),
                new Document("$sort",
                        new Document("count", -1L)));
    }

    /**
     *Methode to create mongo aggregation of fractions route
     * @param queryParams
     * @return
     */
    public List<Bson> createFractionsAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                replaceDateStringByDate("date"),
                createMatchByDate("$date",queryParams.get("startDate").value(),queryParams.get("endDate").value()),
                new Document("$lookup",
                        new Document("from", "speakers")
                                .append("localField", "tagesordnungspunkte.reden.rednerID")
                                .append("foreignField", "_id")
                                .append("as", "redner")),
                unwindHelper("$redner"),
                matchHelper("redner.fraktion", null),
                new Document("$group",
                        new Document("_id", "$redner.fraktion")
                                .append("count",
                                        new Document("$sum", 1L))),
                new Document("$sort",
                        new Document("count", -1L)));
    }

    /**
     *Methode to create mongo aggregation of statistic route
     * @param queryParams
     * @return
     */
    public List<Bson> createStatisticAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
                unwindHelper("$tagesordnungspunkte"),
                unwindHelper("$tagesordnungspunkte.reden"),
                new Document("$facet",
                        new Document("persons", Arrays.asList(
                                new Document("$project",
                                        new Document("content",
                                                new Document("$toString", "$tagesordnungspunkte.reden.content"))
                                                .append("id", "$tagesordnungspunkte.reden.redeID")),
                                new Document("$match",
                                        new Document("content",
                                                new Document("$not",
                                                        new Document("$eq",
                                                                new BsonNull())))),
                                new Document("$project",
                                        new Document("_id", 0L)
                                                .append("id", "$id")
                                                .append("length",
                                                        new Document("$strLenCP", "$content"))),
                                new Document("$sort",
                                        new Document("length", -1L))))
                                .append("speakers",Arrays.asList(
                                        new Document("$project",
                                                new Document("id", "$tagesordnungspunkte.reden.rednerID")),
                                        new Document("$group",
                                                new Document("_id", "$id")
                                                        .append("count",
                                                                new Document("$sum", 1))),
                                        new Document("$match",
                                                new Document("_id",
                                                        new Document("$not",
                                                                new Document("$eq",
                                                                        new BsonNull())))),
                                        new Document("$sort",
                                                new Document("count", -1)))))
        );

    }

    /**
     *Methode to create mongo aggregation of pos route
     * @param queryParams
     * @return
     */
    public List<Bson> createPOSAggregation(QueryParamsMap queryParams){
        return Arrays.asList(
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
                unwindHelper("$pos"),
                new Document("$group",
                        new Document("_id", "$pos")
                                .append("count",
                                        new Document("$sum", 1))),
                new Document("$match",
                        new Document("count",
                                new Document("$gte", minimumOfZero(queryParams,"minimum")))),
                new Document("$sort",
                        new Document("count", -1L)));
    }

    /**
     *Methode to create mongo aggregation of full text search route
     * @param queryParamsMap
     * @return
     */
    public List<Bson> createFullTextSearchAggregation(QueryParamsMap queryParamsMap){
        return Arrays.asList(new Document("$match",
                        new Document("tagesordnungspunkte.reden.content",
                                new Document("$exists", true))));
    }




}

