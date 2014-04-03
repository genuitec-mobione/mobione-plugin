//
//  MobiOnePlugin.h
//  WebApp
//
//  Created by Lonnie Pryor on 4/13/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <MediaPlayer/MediaPlayer.h>

@interface MobiOnePlugin : CDVPlugin
{
    NSString* callbackID;  
    @private UIWebView *webview;
}

@property (nonatomic, copy) NSString* callbackID;


- (void) useNativeDialPhone:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options;


- (void) dialPhone:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options;


- (void) useNativeVideoPlayer:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options;


- (void) playVideo:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options;

- (void) movieExitedFullscreen:(NSNotification*)notification;

- (void) moviePlaybackComplete:(NSNotification *)notification;

- (void) disposeMoviePlayerController:(MPMoviePlayerController *)moviePlayerController;

@end
