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
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd
                                                                                           target:self
                                                                                           action:@selector(addTask)];
    [self requery];
}

- (void) addTask {
    UIAlertController* alert = [UIAlertController alertControllerWithTitle:@"New task"
                                                                   message:nil
                                                            preferredStyle:UIAlertControllerStyleAlert];
    [alert addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
        textField.placeholder = @"Title";
    }];
    [alert addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
        textField.placeholder = @"Tags (comma separated)";
    }];
    
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"Cancel"
                                                           style:UIAlertActionStyleCancel
                                                         handler:^(UIAlertAction * action) {}];
    UIAlertAction *createTaskAction = [UIAlertAction actionWithTitle:@"Create task"
                                                               style:UIAlertActionStyleDefault
                                                             handler:^(UIAlertAction * action) {
                                                                 dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                                                                     [[SDBSampleUtilsTaskUtils getInstance] insertNewTaskWithNSString:[alert.textFields objectAtIndex:0].text withInt:0 withLong:0 withNSString:[alert.textFields objectAtIndex:1].text];
                                                                 });
                                                             }];
    
    [alert addAction:cancelAction];
    [alert addAction:createTaskAction];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) deliverResult:(SDBSquidCursor *)cursor {
    SDBSquidCursor *oldCursor = self.tasksCursor;
    self.tasksCursor = cursor;
    [oldCursor close];
    [self.tableView reloadData];
}

- (void) requery {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        SDBSquidCursor *cursor = [[SDBSampleUtilsTaskUtils getInstance] getTasksCursor];
        [cursor getCount];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self deliverResult:cursor];
        });
    });
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [self.tasksCursor getCount];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return 60.0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    static NSString *identifier = @"TaskCell";
    
    [self.tasksCursor moveToPositionWithInt:(int)indexPath.row];
    TaskCell *cell = [tableView dequeueReusableCellWithIdentifier:identifier];
    [cell.task readPropertiesFromCursorWithSDBSquidCursor:self.tasksCursor];
    
    if ([cell.task isCompleted]) {
        NSMutableAttributedString *strikethroughTitle = [[NSMutableAttributedString alloc] initWithString:[cell.task getTitle]];
        [strikethroughTitle addAttribute:NSStrikethroughStyleAttributeName
                                   value:@(NSUnderlineStyleSingle)
                                   range:NSMakeRange(0, [strikethroughTitle length])];
        cell.textLabel.attributedText = strikethroughTitle;
    } else {
        cell.textLabel.text = [cell.task getTitle];
    }
    cell.tags.text = [cell.task getWithSDBProperty:SDBSampleUtilsTaskUtils_get_TAGS_CONCAT_()];
    
    return cell;
}

- (void)tableView:(UITableView * _Nonnull)tableView didSelectRowAtIndexPath:(NSIndexPath * _Nonnull)indexPath {
    [self.tasksCursor moveToPositionWithInt:(int) indexPath.row];
    SDBSampleTask *task = [[SDBSampleTask alloc] initWithSDBSquidCursor:self.tasksCursor];
    
    // Create complete/delete/cancel dialog
    UIAlertController* alert = [UIAlertController alertControllerWithTitle:[task getTitle]
                                                                   message:nil
                                                            preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction* cancelAction = [UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel
                                                          handler:^(UIAlertAction * action) {}];
    UIAlertAction *deleteAction = [UIAlertAction actionWithTitle:@"Delete" style:UIAlertActionStyleDestructive
                                                         handler:^(UIAlertAction * action) {
                                                             dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                                                                 [[SDBSampleUtilsTaskUtils getInstance] deleteTaskWithSDBSampleTask:task];
                                                             });
                                                         }];
    UIAlertAction *completeAction = [UIAlertAction actionWithTitle:@"Complete" style:UIAlertActionStyleDefault
                                                           handler:^(UIAlertAction * action) {
                                                               dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                                                                   [[SDBSampleUtilsTaskUtils getInstance] completeTaskWithSDBSampleTask:task];
                                                               });
                                                           }];
    
    [alert addAction:cancelAction];
    [alert addAction:deleteAction];
    [alert addAction:completeAction];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void) dealloc {
    [self.tasksCursor close];
    self.tasksCursor = nil;
}

@end
