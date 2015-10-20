//
//  SQLitePreparedStatement.m
//  j2objc-squidb-experiments
//
//  Created by Sam Bosley on 10/16/15.
//  Copyright © 2015 Sam Bosley. All rights reserved.
//

#import "SQLitePreparedStatement.h"

@implementation SQLitePreparedStatement

@synthesize statement;

- (id) initWithStatement:(sqlite3_stmt *)_statement {
    if (self = [super init])  {
        self.statement = _statement;
    }
    return self;
}

@end
