/**
 * 
 * 3 b (1)
 * 
 * Sentiment Scatter chart (as mentioned int the tutorium)
 * 
 * 
 * 
 */

//Some variables used in this script
var partyFilterSentiment = "";
var fractionFilterSentiment = "";
var speakerFilterSentiment = "";

var ctxSentiment;
var ChartSentiment;

var firsTimeSentient = true;


//sets the Party Filter
function FilterPartySentiment(){
    var e = document.getElementById("selectPartySentiment")
    if(e.value === "All"){
        partyFilterSentiment=""
        fractionFilterSentiment=""
        speakerFilterSentiment=""
    }else{
        partyFilterSentiment = "?party="+e.value;
        fractionFilterSentiment=""
        speakerFilterSentiment=""
    }
    mainSentiment();
}

//sets the Fraction Filter
function FilterFractionSentiment(){
    var e = document.getElementById("selectFractionSentiment")
    if(e.value === "All"){
        partyFilterSentiment=""
        fractionFilterSentiment=""
        speakerFilterSentiment=""
    }else{
        partyFilterSentiment = "";
        fractionFilterSentiment="?fraction="+e.value;
        speakerFilterSentiment=""
    }
    mainSentiment();

}

//sets the Speaker Filter
function FilterSpeakerSentiment(){
    var e = document.getElementById("RednerValueSentiment")
    if(e.value === ""){
        partyFilterSentiment=""
        fractionFilterSentiment=""
        speakerFilterSentiment=""
    }else{
        partyFilterSentiment = "";
        fractionFilterSentiment="";
        speakerFilterSentiment="?user="+e.value;
    }
    mainSentiment();
}




//Main Sentiment funtion
function mainSentiment(){
$.ajax({
    url: "http://api.prg2021.texttechnologylab.org/sentiment"+speakerFilterSentiment+fractionFilterSentiment+partyFilterSentiment,
    type: "GET",
    dataType: "json",
    success: async function (sentiments) {

        //put the data into a format which is used by the scatter chart
        var result = sentiments.result
        var data = []
        
        var neutral_count = 0;
        var positive_count = 0;
        var negative_count = 0;
        result.forEach(e => {
            // I take the log of the data for better visailisation
           // data.push({x: e.sentiment, y: Math.log(e.count)})

           if(parseFloat(e.sentiment) > 0){
                positive_count = positive_count + e.count;
           }else if(parseFloat(e.sentiment) < 0){
                negative_count = negative_count + e.count;
           }else{
                neutral_count = neutral_count + e.count;
           }
        });

        data = [Math.log(Math.log(neutral_count)),Math.log(Math.log(positive_count)),Math.log(Math.log(negative_count))]
        label=["neutral", "positive", "negative"]

        if(firsTimeSentient){//first time the cahrt is assigend
        ctxSentiment = document.getElementById('chart_sentiment').getContext('2d');
        ChartSentiment = new Chart(ctxSentiment, {
            type: 'radar',
            data: {
                labels: label,
                datasets: [{
                    label: '',
                    data: data,
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    fill: true,
                }],
            }
        })

        firsTimeSentient = false;
        }else{
            //if the chart gets updated by a new filter
            ChartSentiment.data = {
                datasets: [{
                    label: '# data',
                    data: data,
                    backgroundColor: 'rgba(54, 162, 235, 0.2)',
                    fill: false,
                }]
            }
            ChartSentiment.update();

        };
    },
    error: function(params) {
        console.log("params")
    }
})
}

mainSentiment();