/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 70.46783625730994, "KoPercent": 29.53216374269006};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.7035672514619883, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.0, 500, 1500, "GET /api/analytics/adherence/1"], "isController": false}, {"data": [0.998, 500, 1500, "GET /api/tests"], "isController": false}, {"data": [0.998, 500, 1500, "GET /api/users"], "isController": false}, {"data": [0.9975, 500, 1500, "GET /api/predictions/user/1"], "isController": false}, {"data": [0.9995, 500, 1500, "GET /api/users/1"], "isController": false}, {"data": [0.0, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.9977777777777778, 500, 1500, "GET /api/treatment-plans/user/1"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/treatment-plans/user/1/today-tasks"], "isController": false}, {"data": [0.0, 500, 1500, "GET /api/tests/history/1"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 8550, 2525, 29.53216374269006, 27.658011695906453, 1, 1099, 10.0, 51.0, 100.0, 334.4899999999998, 141.54223090421482, 456.32580217610837, 27.273942416730126], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["GET /api/analytics/adherence/1", 400, 400, 100.0, 34.45749999999997, 6, 475, 11.0, 73.0, 143.39999999999986, 347.82000000000016, 13.821222487128987, 7.985636456497702, 2.6994575170173802], "isController": false}, {"data": ["GET /api/tests", 500, 0, 0.0, 29.982, 5, 720, 9.0, 59.900000000000034, 129.89999999999998, 366.5900000000004, 16.783029001074116, 13.750938800684747, 3.0157005236305046], "isController": false}, {"data": ["GET /api/users", 2000, 0, 0.0, 21.525000000000006, 1, 782, 8.0, 34.0, 61.94999999999982, 293.0, 33.11971119611837, 55.3720171560104, 5.951198105552519], "isController": false}, {"data": ["GET /api/predictions/user/1", 400, 0, 0.0, 39.345, 8, 930, 14.0, 68.7000000000001, 168.09999999999957, 482.0400000000009, 13.39674459106437, 16.7851790139996, 2.577303402773126], "isController": false}, {"data": ["GET /api/users/1", 2000, 0, 0.0, 16.55799999999999, 1, 723, 7.0, 29.0, 47.0, 193.98000000000002, 33.515433857291285, 28.736866139357176, 6.087764352984549], "isController": false}, {"data": ["POST /api/auth/login", 500, 500, 100.0, 41.117999999999945, 5, 757, 12.0, 102.4000000000002, 190.39999999999986, 466.18000000000075, 17.355687458780242, 9.502849044569405, 4.525359913568677], "isController": false}, {"data": ["GET /api/treatment-plans/user/1", 1125, 0, 0.0, 36.19644444444438, 3, 1099, 14.0, 63.0, 136.20000000000027, 388.74, 25.540319651289504, 457.2315818822648, 5.013285400301943], "isController": false}, {"data": ["GET /api/treatment-plans/user/1/today-tasks", 1125, 1125, 100.0, 34.41333333333332, 1, 792, 11.0, 60.0, 130.4000000000001, 436.5000000000002, 26.19019904551275, 15.466471998312187, 5.447766012396694], "isController": false}, {"data": ["GET /api/tests/history/1", 500, 500, 100.0, 31.606, 5, 402, 12.0, 66.90000000000003, 120.74999999999994, 309.9100000000001, 17.19276528436834, 9.834396061137474, 3.257223110515095], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 2025, 80.1980198019802, 23.68421052631579], "isController": false}, {"data": ["404", 500, 19.801980198019802, 5.847953216374269], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 8550, 2525, "500", 2025, "404", 500, "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["GET /api/analytics/adherence/1", 400, 400, "500", 400, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/auth/login", 500, 500, "404", 500, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": ["GET /api/treatment-plans/user/1/today-tasks", 1125, 1125, "500", 1125, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/tests/history/1", 500, 500, "500", 500, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
