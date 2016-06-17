/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
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

