//
//  SimpleTasksNotifier.h
//  squidb-ios-sample
//
//  Created by Sam Bosley on 11/4/15.
//  Copyright © 2015 Yahoo. All rights reserved.
//

#import "SimpleDataChangedNotifier.h"
#import "TasksViewController.h"

@interface SimpleTasksNotifier : ComYahooSquidbDataSimpleDataChangedNotifier

@property (nonatomic, weak) TasksViewController *tasksViewController;

@end
