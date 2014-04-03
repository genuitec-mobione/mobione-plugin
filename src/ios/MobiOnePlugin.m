#import "MobiOnePlugin.h"

@implementation MobiOnePlugin

@synthesize callbackID;

- (id)init
{
    self = [super init];
    if (self)
    {
        webview = nil;
    }
    return self;
}

- (void) useNativeDialPhone:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options {
    self.callbackID = [arguments pop];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"true"];
    [self writeJavascript: [pluginResult toSuccessCallbackString:self.callbackID]];
}

- (void) dialPhone:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    NSString* ph = [options valueForKey:@"number"];
    UIDevice *device = [UIDevice currentDevice];
    if ([[device model] isEqualToString:@"iPhone"] ) {
        if (webview != nil) {
            webview = nil;
        }
        webview = [[UIWebView alloc] init];
        NSURL *telURL = [NSURL URLWithString:[NSString stringWithFormat:@"tel:%@", ph]];
        [webview loadRequest:[NSURLRequest requestWithURL:telURL]];
    } else {
        UIAlertView *Notpermitted=[[UIAlertView alloc] initWithTitle:@"Alert" message:@"Your device doesn't support this feature." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [Notpermitted show];
    }
}


- (void) useNativeVideoPlayer:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options {
    self.callbackID = [arguments pop];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"true"];
    [self writeJavascript: [pluginResult toSuccessCallbackString:self.callbackID]];
}


- (void) playVideo:(NSMutableArray *)arguments withDict:(NSMutableDictionary *) options {    
    NSLog(@"playVideo called");
    
    NSString* filepath = [[[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:@"www"] stringByAppendingPathComponent:[arguments objectAtIndex:1]];
    
    NSURL* fileURL = [NSURL fileURLWithPath:filepath];
    
    // AY: There's a bug in apple's framework with with GraphicContext, resulting in some error messages in console when this view is showing up
    // so it can be wrapped to artificial context to avoid this, however, looks like it harmless so we'll skip this for now
    
    // AY: TODO: check that player view is not leaking here
    
    MPMoviePlayerViewController *mpvController = [[MPMoviePlayerViewController alloc] initWithContentURL:fileURL];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(movieExitedFullscreen:)
                                                 name:MPMoviePlayerWillExitFullscreenNotification
                                               object:mpvController.moviePlayer];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(moviePlaybackComplete:)
                                                 name:MPMoviePlayerPlaybackDidFinishNotification
                                               object:mpvController.moviePlayer];
        
    [self.viewController presentMoviePlayerViewControllerAnimated:mpvController];
}

- (void)movieExitedFullscreen:(NSNotification*)notification {
    //NSLog(@"movieExitedFullscreen");
    [self disposeMoviePlayerController: [notification object]];
}

- (void)moviePlaybackComplete:(NSNotification *)notification {
    //NSLog(@"moviePlaybackComplete");
    [self disposeMoviePlayerController: [notification object]];
}

- (void)disposeMoviePlayerController:(MPMoviePlayerController *)playerController {
    //NSLog(@"Killing video. 1");
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:MPMoviePlayerWillExitFullscreenNotification
                                                  object:playerController];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:MPMoviePlayerPlaybackDidFinishNotification
                                                  object:playerController];

    [playerController stop];
    
    // AY: looks like other actions (hiding of view, returning back to parent view and all cleanup) are done by player view controller
}


- (void)dealloc
{
    if (webview != nil) {
        webview = nil;
    }
}

@end
