/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
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
