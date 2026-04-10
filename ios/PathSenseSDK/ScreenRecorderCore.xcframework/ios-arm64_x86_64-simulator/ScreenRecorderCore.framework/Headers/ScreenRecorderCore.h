#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SRCKotlinEnumCompanion, SRCKotlinEnum<E>, SRCBubblePosition, SRCKotlinArray<T>, SRCOutputFormat, SRCPermissionType, SRCRecordingError, SRCRecordingEvent, SRCRecordingEventBubbleHidden, SRCRecordingEventBubbleShown, SRCRecordingEventDurationUpdate, SRCRecordingEventPermissionDenied, SRCRecordingEventPermissionGranted, SRCRecordingEventPermissionRequired, SRCRecordingEventRecordingFailed, SRCRecordingEventRecordingStarted, SRCRecordingFile, SRCRecordingEventRecordingStopped, SRCRecordingState, SRCScreenRecorderCompanion, SRCScreenRecorderConfig, SRCVideoQuality;

@protocol SRCKotlinComparable, SRCKotlinIterator;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface SRCBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SRCBase (SRCBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SRCMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SRCMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSRCKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SRCNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface SRCByte : SRCNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SRCUByte : SRCNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SRCShort : SRCNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SRCUShort : SRCNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SRCInt : SRCNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SRCUInt : SRCNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SRCLong : SRCNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SRCULong : SRCNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SRCFloat : SRCNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SRCDouble : SRCNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SRCBoolean : SRCNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SRCKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SRCKotlinEnum<E> : SRCBase <SRCKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SRCKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BubblePosition")))
@interface SRCBubblePosition : SRCKotlinEnum<SRCBubblePosition *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SRCBubblePosition *leadingCenter __attribute__((swift_name("leadingCenter")));
@property (class, readonly) SRCBubblePosition *trailingCenter __attribute__((swift_name("trailingCenter")));
@property (class, readonly) SRCBubblePosition *leadingTop __attribute__((swift_name("leadingTop")));
@property (class, readonly) SRCBubblePosition *trailingTop __attribute__((swift_name("trailingTop")));
@property (class, readonly) SRCBubblePosition *leadingBottom __attribute__((swift_name("leadingBottom")));
@property (class, readonly) SRCBubblePosition *trailingBottom __attribute__((swift_name("trailingBottom")));
+ (SRCKotlinArray<SRCBubblePosition *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SRCBubblePosition *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OutputFormat")))
@interface SRCOutputFormat : SRCKotlinEnum<SRCOutputFormat *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SRCOutputFormat *mp4 __attribute__((swift_name("mp4")));
@property (class, readonly) SRCOutputFormat *mov __attribute__((swift_name("mov")));
+ (SRCKotlinArray<SRCOutputFormat *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SRCOutputFormat *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *fileExtension __attribute__((swift_name("fileExtension")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PermissionType")))
@interface SRCPermissionType : SRCKotlinEnum<SRCPermissionType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SRCPermissionType *overlay __attribute__((swift_name("overlay")));
@property (class, readonly) SRCPermissionType *screenCapture __attribute__((swift_name("screenCapture")));
@property (class, readonly) SRCPermissionType *microphone __attribute__((swift_name("microphone")));
+ (SRCKotlinArray<SRCPermissionType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SRCPermissionType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("RecordingError")))
@interface SRCRecordingError : SRCBase
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingError.DiskFull")))
@interface SRCRecordingErrorDiskFull : SRCRecordingError
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingError.EncoderFailed")))
@interface SRCRecordingErrorEncoderFailed : SRCRecordingError
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingError.MaxDurationReached")))
@interface SRCRecordingErrorMaxDurationReached : SRCRecordingError
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingError.PermissionDenied")))
@interface SRCRecordingErrorPermissionDenied : SRCRecordingError
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingError.SystemUnavailable")))
@interface SRCRecordingErrorSystemUnavailable : SRCRecordingError
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("RecordingEvent")))
@interface SRCRecordingEvent : SRCBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.BubbleHidden")))
@interface SRCRecordingEventBubbleHidden : SRCRecordingEvent
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bubbleHidden __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SRCRecordingEventBubbleHidden *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.BubbleShown")))
@interface SRCRecordingEventBubbleShown : SRCRecordingEvent
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bubbleShown __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SRCRecordingEventBubbleShown *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.DurationUpdate")))
@interface SRCRecordingEventDurationUpdate : SRCRecordingEvent
- (instancetype)initWithSessionId:(NSString *)sessionId elapsedMs:(int64_t)elapsedMs __attribute__((swift_name("init(sessionId:elapsedMs:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventDurationUpdate *)doCopySessionId:(NSString *)sessionId elapsedMs:(int64_t)elapsedMs __attribute__((swift_name("doCopy(sessionId:elapsedMs:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t elapsedMs __attribute__((swift_name("elapsedMs")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.PermissionDenied")))
@interface SRCRecordingEventPermissionDenied : SRCRecordingEvent
- (instancetype)initWithType:(SRCPermissionType *)type __attribute__((swift_name("init(type:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventPermissionDenied *)doCopyType:(SRCPermissionType *)type __attribute__((swift_name("doCopy(type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SRCPermissionType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.PermissionGranted")))
@interface SRCRecordingEventPermissionGranted : SRCRecordingEvent
- (instancetype)initWithType:(SRCPermissionType *)type __attribute__((swift_name("init(type:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventPermissionGranted *)doCopyType:(SRCPermissionType *)type __attribute__((swift_name("doCopy(type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SRCPermissionType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.PermissionRequired")))
@interface SRCRecordingEventPermissionRequired : SRCRecordingEvent
- (instancetype)initWithType:(SRCPermissionType *)type __attribute__((swift_name("init(type:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventPermissionRequired *)doCopyType:(SRCPermissionType *)type __attribute__((swift_name("doCopy(type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SRCPermissionType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.RecordingFailed")))
@interface SRCRecordingEventRecordingFailed : SRCRecordingEvent
- (instancetype)initWithSessionId:(NSString *)sessionId error:(SRCRecordingError *)error __attribute__((swift_name("init(sessionId:error:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventRecordingFailed *)doCopySessionId:(NSString *)sessionId error:(SRCRecordingError *)error __attribute__((swift_name("doCopy(sessionId:error:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SRCRecordingError *error __attribute__((swift_name("error")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.RecordingStarted")))
@interface SRCRecordingEventRecordingStarted : SRCRecordingEvent
- (instancetype)initWithSessionId:(NSString *)sessionId __attribute__((swift_name("init(sessionId:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventRecordingStarted *)doCopySessionId:(NSString *)sessionId __attribute__((swift_name("doCopy(sessionId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingEvent.RecordingStopped")))
@interface SRCRecordingEventRecordingStopped : SRCRecordingEvent
- (instancetype)initWithSessionId:(NSString *)sessionId file:(SRCRecordingFile *)file __attribute__((swift_name("init(sessionId:file:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingEventRecordingStopped *)doCopySessionId:(NSString *)sessionId file:(SRCRecordingFile *)file __attribute__((swift_name("doCopy(sessionId:file:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SRCRecordingFile *file __attribute__((swift_name("file")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingFile")))
@interface SRCRecordingFile : SRCBase
- (instancetype)initWithPath:(NSString *)path durationMs:(int64_t)durationMs fileSizeBytes:(int64_t)fileSizeBytes width:(int32_t)width height:(int32_t)height __attribute__((swift_name("init(path:durationMs:fileSizeBytes:width:height:)"))) __attribute__((objc_designated_initializer));
- (SRCRecordingFile *)doCopyPath:(NSString *)path durationMs:(int64_t)durationMs fileSizeBytes:(int64_t)fileSizeBytes width:(int32_t)width height:(int32_t)height __attribute__((swift_name("doCopy(path:durationMs:fileSizeBytes:width:height:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t durationMs __attribute__((swift_name("durationMs")));
@property (readonly) int64_t fileSizeBytes __attribute__((swift_name("fileSizeBytes")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) NSString *path __attribute__((swift_name("path")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordingState")))
@interface SRCRecordingState : SRCKotlinEnum<SRCRecordingState *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SRCRecordingState *idle __attribute__((swift_name("idle")));
@property (class, readonly) SRCRecordingState *requestingPermission __attribute__((swift_name("requestingPermission")));
@property (class, readonly) SRCRecordingState *recording __attribute__((swift_name("recording")));
@property (class, readonly) SRCRecordingState *stopping __attribute__((swift_name("stopping")));
+ (SRCKotlinArray<SRCRecordingState *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SRCRecordingState *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScreenRecorder")))
@interface SRCScreenRecorder : SRCBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) SRCScreenRecorderCompanion *companion __attribute__((swift_name("companion")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScreenRecorder.Companion")))
@interface SRCScreenRecorderCompanion : SRCBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SRCScreenRecorderCompanion *shared __attribute__((swift_name("shared")));
- (void)destroy __attribute__((swift_name("destroy()")));
- (void)hide __attribute__((swift_name("hide()")));
- (void)onBubbleTapRecord __attribute__((swift_name("onBubbleTapRecord()")));
- (void)onBubbleTapStop __attribute__((swift_name("onBubbleTapStop()")));
- (void)onPermissionResultGranted:(BOOL)granted __attribute__((swift_name("onPermissionResult(granted:)")));
- (void)show __attribute__((swift_name("show()")));
- (void)startConfig:(SRCScreenRecorderConfig *)config __attribute__((swift_name("start(config:)")));
@property (readonly) SRCRecordingState *state __attribute__((swift_name("state")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScreenRecorderConfig")))
@interface SRCScreenRecorderConfig : SRCBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithTintColor:(int64_t)tintColor bubblePosition:(SRCBubblePosition *)bubblePosition audioEnabled:(BOOL)audioEnabled videoQuality:(SRCVideoQuality *)videoQuality maxDurationSec:(int32_t)maxDurationSec outputFormat:(SRCOutputFormat *)outputFormat listener:(void (^ _Nullable)(SRCRecordingEvent *))listener pathSenseEnabled:(BOOL)pathSenseEnabled __attribute__((swift_name("init(tintColor:bubblePosition:audioEnabled:videoQuality:maxDurationSec:outputFormat:listener:pathSenseEnabled:)"))) __attribute__((objc_designated_initializer));
- (SRCScreenRecorderConfig *)doCopyTintColor:(int64_t)tintColor bubblePosition:(SRCBubblePosition *)bubblePosition audioEnabled:(BOOL)audioEnabled videoQuality:(SRCVideoQuality *)videoQuality maxDurationSec:(int32_t)maxDurationSec outputFormat:(SRCOutputFormat *)outputFormat listener:(void (^ _Nullable)(SRCRecordingEvent *))listener pathSenseEnabled:(BOOL)pathSenseEnabled __attribute__((swift_name("doCopy(tintColor:bubblePosition:audioEnabled:videoQuality:maxDurationSec:outputFormat:listener:pathSenseEnabled:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property BOOL audioEnabled __attribute__((swift_name("audioEnabled")));
@property SRCBubblePosition *bubblePosition __attribute__((swift_name("bubblePosition")));
@property void (^ _Nullable listener)(SRCRecordingEvent *) __attribute__((swift_name("listener")));
@property int32_t maxDurationSec __attribute__((swift_name("maxDurationSec")));
@property SRCOutputFormat *outputFormat __attribute__((swift_name("outputFormat")));
@property BOOL pathSenseEnabled __attribute__((swift_name("pathSenseEnabled")));
@property int64_t tintColor __attribute__((swift_name("tintColor")));
@property SRCVideoQuality *videoQuality __attribute__((swift_name("videoQuality")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VideoQuality")))
@interface SRCVideoQuality : SRCKotlinEnum<SRCVideoQuality *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SRCVideoQuality *deviceNative __attribute__((swift_name("deviceNative")));
@property (class, readonly) SRCVideoQuality *sd480 __attribute__((swift_name("sd480")));
@property (class, readonly) SRCVideoQuality *hd720 __attribute__((swift_name("hd720")));
@property (class, readonly) SRCVideoQuality *fhd1080 __attribute__((swift_name("fhd1080")));
+ (SRCKotlinArray<SRCVideoQuality *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SRCVideoQuality *> *entries __attribute__((swift_name("entries")));
@property (readonly) float bitrateMbps __attribute__((swift_name("bitrateMbps")));
@property (readonly) int32_t height __attribute__((swift_name("height")));
@property (readonly) int32_t width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SRCKotlinEnumCompanion : SRCBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SRCKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SRCKotlinArray<T> : SRCBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SRCInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SRCKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SRCKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
