//
//  ViewController.m
//  squidb-ios-sample
//
//  Created by Sam Bosley on 11/3/15.
//  Copyright © 2015 Yahoo. All rights reserved.
//

#import "TasksViewController.h"
#import "Property.h"
#import "Task.h"
#import "Tag.h"
#import "TaskUtils.h"

@interface TasksViewController ()

@end

@implementation TasksViewController

@synthesize tasksCursor;

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    self.tasksCursor = [[ComYahooSquidbSampleUtilsTaskUtils getInstance] getTasksCursor];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [tasksCursor getCount];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return 60.0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    static NSString *identifier = @"TaskCell";
    
    [self.tasksCursor moveToPositionWithInt:(int)indexPath.row];
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:identifier];
    
    NSString *taskTitle = [self.tasksCursor getWithComYahooSquidbSqlProperty:ComYahooSquidbSampleModelsTask_TITLE_];
    NSString *taskTags = @"Task tags";//[self.tasksCursor getWithComYahooSquidbSqlProperty:ComYahooSquidbSampleUtilsTaskUtils_TAGS_CONCAT_];
    
    if (taskTags) {
        cell.textLabel.text = [taskTitle stringByAppendingString:[NSString stringWithFormat:@"\n%@", taskTags]];
    } else {
        cell.textLabel.text = taskTitle;
    }
    
    return cell;
}

@end
