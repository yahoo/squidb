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
#import "TaskCell.h"

@interface TasksViewController ()

@end

@implementation TasksViewController

@synthesize tasksCursor;

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    [self requery];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) deliverResult:(ComYahooSquidbDataSquidCursor *)cursor {
    self.tasksCursor = cursor;
    [self.tableView reloadData];
}

- (void) requeryInBackground {
    ComYahooSquidbDataSquidCursor *cursor = [[ComYahooSquidbSampleUtilsTaskUtils getInstance] getTasksCursor];
    [cursor getCount];
    [self performSelectorOnMainThread:@selector(deliverResult:) withObject:cursor waitUntilDone:NO];
}

- (void) requery {
    [self performSelectorInBackground:@selector(requeryInBackground) withObject:nil];
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
    TaskCell *cell = [tableView dequeueReusableCellWithIdentifier:identifier];
    
    NSString *taskTitle = [self.tasksCursor getWithComYahooSquidbSqlProperty:ComYahooSquidbSampleModelsTask_TITLE_];
    NSString *taskTags = [self.tasksCursor getWithComYahooSquidbSqlProperty:ComYahooSquidbSampleUtilsTaskUtils_TAGS_CONCAT_];
    
    cell.textLabel.text = [taskTitle stringByAppendingString:[NSString stringWithFormat:@"\n%@", taskTags]];
    cell.tags.text = taskTags;
    
    return cell;
}

@end
