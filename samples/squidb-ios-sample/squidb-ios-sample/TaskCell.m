//
//  TaskCell.m
//  squidb-ios-sample
//
//  Created by Sam Bosley on 11/4/15.
//  Copyright © 2015 Yahoo. All rights reserved.
//

#import "TaskCell.h"

@implementation TaskCell

@synthesize task = _task;
@synthesize tags;

- (SDBSampleTask *) task {
    if (!_task) {
        _task = [[SDBSampleTask alloc] init];
    }
    return _task;
}

- (void)awakeFromNib {
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated {
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

@end
