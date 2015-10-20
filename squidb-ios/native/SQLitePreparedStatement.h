//
//  SQLitePreparedStatement.h
//  j2objc-squidb-experiments
//
//  Created by Sam Bosley on 10/16/15.
//  Copyright © 2015 Sam Bosley. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <sqlite3.h>

@interface SQLitePreparedStatement : NSObject

@property sqlite3_stmt *statement;

- (id) initWithStatement:(sqlite3_stmt *)_statement;

@end
