#import <Foundation/Foundation.h>

NSBundle* SDWebImage_SWIFTPM_MODULE_BUNDLE() {
    NSURL *bundleURL = [[[NSBundle mainBundle] bundleURL] URLByAppendingPathComponent:@"SDWebImage_SDWebImage.bundle"];

    NSBundle *preferredBundle = [NSBundle bundleWithURL:bundleURL];
    if (preferredBundle == nil) {
      return [NSBundle bundleWithPath:@"/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements/Renderers/iOS/.build/arm64-apple-macosx/debug/SDWebImage_SDWebImage.bundle"];
    }

    return preferredBundle;
}