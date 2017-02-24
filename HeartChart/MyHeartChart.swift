//
//  MyHeartChart.swift
//  HeartChart
//
//  Created by Larry Bulen on 2/24/17.
//  Copyright © 2017 Larry Bulen. All rights reserved.
//

import UIKit
import Charts

// CLASS MYHEARTCHART
// Subclassed the 'chartView', so I could put some of the 'styling' here instead of the ViewController

class MyHeartChart : LineChartView {
    var chartView : LineChartView? = LineChartView()
    
    //
    // INIT?(CODER ADECODER: NSCODER)
    // Designated init method; calls CreateChart
    //
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        createChart()
    }
    
    //
    // FUNC CREATECHART()
    // Create the initial chart instance
    //
    func createChart() {
        chartView?.chartDescription?.enabled = false;
        
        chartView?.dragEnabled = true;
        chartView?.setScaleEnabled(true)
        chartView?.pinchZoomEnabled = true;
        chartView?.drawGridBackgroundEnabled = false;
        
        // x-axis limit line
        let llXAxis : ChartLimitLine = ChartLimitLine.init(limit: 10.0, label: "Index 10")
        llXAxis.lineWidth = 4.0;
        llXAxis.lineDashLengths = [10.0, 10.0, 0.0];
        llXAxis.labelPosition = .rightBottom
        llXAxis.valueFont = UIFont(name: "HelveticaNeue-Light", size: 10)!
        
        chartView?.xAxis.gridLineDashLengths = [10.0, 10.0];
        chartView?.xAxis.gridLineDashPhase = 0.0;
        
        let marker : MarkerImage = MarkerImage.init()
        marker.chartView = chartView;
        chartView?.marker = marker;
        
        chartView?.legend.form = Legend.Form(rawValue: 5)!
        
    }
}
