//
//  HeartChartViewController.swift
//  HeartChart
//
//  Created by Larry Bulen on 2/24/17.
//  Copyright © 2017 Larry Bulen. All rights reserved.
//


import UIKit


// CLASS HEARTCHARTVIEWCONTROLLER
// Central class for this app
// We present the user with a 'heart chart' which graphs the users pulse; Age field
// NOTE: this class requires 2 - 3rd party libraries
// 1) BBLE - used to connect to a Beets BLU heart rate monitor; this library is provided by (http://beetsblu.com)
// 2) CHARTS - used to draw a line graph

class HeartChartViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    
    @IBOutlet weak var myTableView: UITableView!          // used for tableview
    
    //
    // INIT?(CODER ADECODER: NSCODER)
    // Required initializer for this class
    //
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableViewAutomaticDimension
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = myTableView.dequeueReusableCell(withIdentifier: "HCTableViewCell", for: indexPath) as! HeartChartTableViewCell
        return cell
    }
}
