/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
//
//  TaskCell.h
//  squidb-ios-sample
//
//  Created by Sam Bosley on 11/4/15.
//  Copyright © 2015 Yahoo. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "Task.h"

@interface TaskCell : UITableViewCell

@property (nonatomic, weak) IBOutlet UILabel *tags;

@property (nonatomic, strong, readonly) SDBSampleTask *task;

@end
