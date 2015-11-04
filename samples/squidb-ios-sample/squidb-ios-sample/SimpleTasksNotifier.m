//
//  SimpleTasksNotifier.m
//  squidb-ios-sample
//
//  Created by Sam Bosley on 11/4/15.
//  Copyright © 2015 Yahoo. All rights reserved.
//

#import "SimpleTasksNotifier.h"


@implementation SimpleTasksNotifier

@synthesize tasksViewController;

- (void) onDataChanged {
    [self.tasksViewController requery];
}

@end
