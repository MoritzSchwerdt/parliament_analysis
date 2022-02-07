var global_party_filter = ""

function fill_select_party(){
var list = document.getElementById("allOptions")

    $.ajax({
        url:"http://api.prg2021.texttechnologylab.org/speakers",
        type: "GET",
        dataType: "json",
        success: function(speakers) {
            var speakerList = speakers.result;
    
            speakerList.forEach(r => {
                var option = document.createElement('option');
                option.value = r.id
                option.text=r.firstname + " " + r.name;
                list.appendChild(option)
            });
        },
        error: function(redner) {
            console.log("Fehler")
        }
    })


    $.ajax({
        url:"http://api.prg2021.texttechnologylab.org/parties",
        type: "GET",
        dataType: "json",
        success: function(parties) {
           /* //get select statement
            var select = document.getElementById("selectParty");
    
            //The first option is all partys
            select[0] = new Option("Alle Parteien");
            select[0].value = "All"*/
    
    
            var parties1 = parties.result
            //Add other parties
            parties1.forEach(party => {
                var option = document.createElement('option');
                option.value = party.id+" [Partei]"
                option.text = party.id +" [Partei]"
                list.appendChild(option)
                //select[select.length] = new Option ( party.id);
                //select[select.length-1].value = party.id;
            });   
        },
        error: function(parties) {
            console.log("Fehler")
        }
    })

    $.ajax({
        url:"http://api.prg2021.texttechnologylab.org/fractions",
        type: "GET",
        dataType: "json",
        success: function(fractions) { 
            var fractions1 = fractions.result
            fractions1.forEach(fraction => {
                var option = document.createElement('option');
                option.value = fraction.id+ " [Fraktion]"
                option.text = fraction.id + " [Fraktion]"
                list.appendChild(option)
            });    
        },
        error: function(parties) {
            console.log("Fehler")
        }
    })
    

}

function set_global_party_filter(){
        var e = document.getElementById("filterinput");
        //if contains party do something
        //if contains fraction do something
        //if contains redner do something
        if(e.value.includes("[Partei]")){
            global_party_filter = "&party="+ e.value.split(" ")[0]
            console.log(e.value.split(" ")[0])

        }else if(e.value.includes("[Fraktion]")){

        }else{
            global_party_filter = "&user="+e.value;
        }
}

fill_select_party()