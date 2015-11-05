//
//  ViewController.h
//  squidb-ios-sample
//
//  Created by Sam Bosley on 11/3/15.
//  Copyright © 2015 Yahoo. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "SquidCursor.h"

@interface TasksViewController : UITableViewController

@property (nonatomic, strong) SDBSquidCursor *tasksCursor;

- (void) requery;

@end

