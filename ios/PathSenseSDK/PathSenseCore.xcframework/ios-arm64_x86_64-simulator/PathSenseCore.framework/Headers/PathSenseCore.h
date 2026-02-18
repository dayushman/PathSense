#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class PSCGestureType, PSCGestureMatch, PSCPathPoint, PSCKotlinEnumCompanion, PSCKotlinEnum<E>, PSCKotlinArray<T>, PSCPathConfig, PSCPathEvent, PSCPathEventCancelled, PSCPathEventEnded, PSCPathEventGestureRecognized, PSCPathMetrics, PSCPathEventMetricsEnded, PSCPathEventMetricsUpdated, PSCPathEventStarted, PSCPathEventUpdated, PSCRectF;

@protocol PSCKotlinComparable, PSCGestureRecognizer, PSCKotlinIterator;

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
@interface PSCBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface PSCBase (PSCBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface PSCMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface PSCMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorPSCKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface PSCNumber : NSNumber
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
@interface PSCByte : PSCNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface PSCUByte : PSCNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface PSCShort : PSCNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface PSCUShort : PSCNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface PSCInt : PSCNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface PSCUInt : PSCNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface PSCLong : PSCNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface PSCULong : PSCNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface PSCFloat : PSCNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface PSCDouble : PSCNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface PSCBoolean : PSCNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GestureMatch")))
@interface PSCGestureMatch : PSCBase
- (instancetype)initWithType:(PSCGestureType *)type score:(float)score algorithm:(NSString *)algorithm __attribute__((swift_name("init(type:score:algorithm:)"))) __attribute__((objc_designated_initializer));
- (PSCGestureMatch *)doCopyType:(PSCGestureType *)type score:(float)score algorithm:(NSString *)algorithm __attribute__((swift_name("doCopy(type:score:algorithm:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *algorithm __attribute__((swift_name("algorithm")));
@property (readonly) float score __attribute__((swift_name("score")));
@property (readonly) PSCGestureType *type __attribute__((swift_name("type")));
@end

__attribute__((swift_name("GestureRecognizer")))
@protocol PSCGestureRecognizer
@required
- (PSCGestureMatch * _Nullable)recognizePoints:(NSArray<PSCPathPoint *> *)points __attribute__((swift_name("recognize(points:)")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol PSCKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface PSCKotlinEnum<E> : PSCBase <PSCKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) PSCKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GestureType")))
@interface PSCGestureType : PSCKotlinEnum<PSCGestureType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) PSCGestureType *line __attribute__((swift_name("line")));
@property (class, readonly) PSCGestureType *circle __attribute__((swift_name("circle")));
@property (class, readonly) PSCGestureType *rectangle __attribute__((swift_name("rectangle")));
@property (class, readonly) PSCGestureType *zigzag __attribute__((swift_name("zigzag")));
@property (class, readonly) PSCGestureType *unknown __attribute__((swift_name("unknown")));
+ (PSCKotlinArray<PSCGestureType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<PSCGestureType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathConfig")))
@interface PSCPathConfig : PSCBase
- (instancetype)initWithSamplingHz:(int32_t)samplingHz minDistancePx:(float)minDistancePx smoothingWindow:(int32_t)smoothingWindow resampleSpacingPx:(float)resampleSpacingPx maxPoints:(int32_t)maxPoints __attribute__((swift_name("init(samplingHz:minDistancePx:smoothingWindow:resampleSpacingPx:maxPoints:)"))) __attribute__((objc_designated_initializer));
- (PSCPathConfig *)doCopySamplingHz:(int32_t)samplingHz minDistancePx:(float)minDistancePx smoothingWindow:(int32_t)smoothingWindow resampleSpacingPx:(float)resampleSpacingPx maxPoints:(int32_t)maxPoints __attribute__((swift_name("doCopy(samplingHz:minDistancePx:smoothingWindow:resampleSpacingPx:maxPoints:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t maxPoints __attribute__((swift_name("maxPoints")));
@property (readonly) float minDistancePx __attribute__((swift_name("minDistancePx")));
@property (readonly) float resampleSpacingPx __attribute__((swift_name("resampleSpacingPx")));
@property (readonly) int32_t samplingHz __attribute__((swift_name("samplingHz")));
@property (readonly) int32_t smoothingWindow __attribute__((swift_name("smoothingWindow")));
@end

__attribute__((swift_name("PathEvent")))
@interface PSCPathEvent : PSCBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.Cancelled")))
@interface PSCPathEventCancelled : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId __attribute__((swift_name("init(sessionId:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventCancelled *)doCopySessionId:(NSString *)sessionId __attribute__((swift_name("doCopy(sessionId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.Ended")))
@interface PSCPathEventEnded : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId points:(NSArray<PSCPathPoint *> *)points __attribute__((swift_name("init(sessionId:points:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventEnded *)doCopySessionId:(NSString *)sessionId points:(NSArray<PSCPathPoint *> *)points __attribute__((swift_name("doCopy(sessionId:points:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<PSCPathPoint *> *points __attribute__((swift_name("points")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.GestureRecognized")))
@interface PSCPathEventGestureRecognized : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId match:(PSCGestureMatch *)match __attribute__((swift_name("init(sessionId:match:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventGestureRecognized *)doCopySessionId:(NSString *)sessionId match:(PSCGestureMatch *)match __attribute__((swift_name("doCopy(sessionId:match:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) PSCGestureMatch *match __attribute__((swift_name("match")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.MetricsEnded")))
@interface PSCPathEventMetricsEnded : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId metrics:(PSCPathMetrics *)metrics __attribute__((swift_name("init(sessionId:metrics:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventMetricsEnded *)doCopySessionId:(NSString *)sessionId metrics:(PSCPathMetrics *)metrics __attribute__((swift_name("doCopy(sessionId:metrics:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) PSCPathMetrics *metrics __attribute__((swift_name("metrics")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.MetricsUpdated")))
@interface PSCPathEventMetricsUpdated : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId metrics:(PSCPathMetrics *)metrics __attribute__((swift_name("init(sessionId:metrics:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventMetricsUpdated *)doCopySessionId:(NSString *)sessionId metrics:(PSCPathMetrics *)metrics __attribute__((swift_name("doCopy(sessionId:metrics:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) PSCPathMetrics *metrics __attribute__((swift_name("metrics")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.Started")))
@interface PSCPathEventStarted : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId point:(PSCPathPoint *)point __attribute__((swift_name("init(sessionId:point:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventStarted *)doCopySessionId:(NSString *)sessionId point:(PSCPathPoint *)point __attribute__((swift_name("doCopy(sessionId:point:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) PSCPathPoint *point __attribute__((swift_name("point")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathEvent.Updated")))
@interface PSCPathEventUpdated : PSCPathEvent
- (instancetype)initWithSessionId:(NSString *)sessionId points:(NSArray<PSCPathPoint *> *)points __attribute__((swift_name("init(sessionId:points:)"))) __attribute__((objc_designated_initializer));
- (PSCPathEventUpdated *)doCopySessionId:(NSString *)sessionId points:(NSArray<PSCPathPoint *> *)points __attribute__((swift_name("doCopy(sessionId:points:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<PSCPathPoint *> *points __attribute__((swift_name("points")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathMetrics")))
@interface PSCPathMetrics : PSCBase
- (instancetype)initWithLength:(float)length bbox:(PSCRectF *)bbox start:(PSCPathPoint *)start end:(PSCPathPoint *)end avgDirectionDeg:(float)avgDirectionDeg avgSpeed:(float)avgSpeed deltaX:(float)deltaX deltaY:(float)deltaY __attribute__((swift_name("init(length:bbox:start:end:avgDirectionDeg:avgSpeed:deltaX:deltaY:)"))) __attribute__((objc_designated_initializer));
- (PSCPathMetrics *)doCopyLength:(float)length bbox:(PSCRectF *)bbox start:(PSCPathPoint *)start end:(PSCPathPoint *)end avgDirectionDeg:(float)avgDirectionDeg avgSpeed:(float)avgSpeed deltaX:(float)deltaX deltaY:(float)deltaY __attribute__((swift_name("doCopy(length:bbox:start:end:avgDirectionDeg:avgSpeed:deltaX:deltaY:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float avgDirectionDeg __attribute__((swift_name("avgDirectionDeg")));
@property (readonly) float avgSpeed __attribute__((swift_name("avgSpeed")));
@property (readonly) PSCRectF *bbox __attribute__((swift_name("bbox")));
@property (readonly) float deltaX __attribute__((swift_name("deltaX")));
@property (readonly) float deltaY __attribute__((swift_name("deltaY")));
@property (readonly) PSCPathPoint *end __attribute__((swift_name("end")));
@property (readonly) float length __attribute__((swift_name("length")));
@property (readonly) PSCPathPoint *start __attribute__((swift_name("start")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathPoint")))
@interface PSCPathPoint : PSCBase
- (instancetype)initWithX:(float)x y:(float)y tMillis:(int64_t)tMillis __attribute__((swift_name("init(x:y:tMillis:)"))) __attribute__((objc_designated_initializer));
- (PSCPathPoint *)doCopyX:(float)x y:(float)y tMillis:(int64_t)tMillis __attribute__((swift_name("doCopy(x:y:tMillis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t tMillis __attribute__((swift_name("tMillis")));
@property (readonly) float x __attribute__((swift_name("x")));
@property (readonly) float y __attribute__((swift_name("y")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PathTracker")))
@interface PSCPathTracker : PSCBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithConfig:(PSCPathConfig *)config __attribute__((swift_name("init(config:)"))) __attribute__((objc_designated_initializer));
- (void)addRecognizerR:(id<PSCGestureRecognizer>)r __attribute__((swift_name("addRecognizer(r:)")));
- (void)clearPoints __attribute__((swift_name("clearPoints()")));
- (void)onCancel __attribute__((swift_name("onCancel()")));
- (void)onDownP:(PSCPathPoint *)p __attribute__((swift_name("onDown(p:)")));
- (void)onMoveP:(PSCPathPoint *)p __attribute__((swift_name("onMove(p:)")));
- (void)onUpP:(PSCPathPoint *)p __attribute__((swift_name("onUp(p:)")));
- (void)removeRecognizerR:(id<PSCGestureRecognizer>)r __attribute__((swift_name("removeRecognizer(r:)")));
@property (readonly) NSArray<PSCPathPoint *> *currentPoints __attribute__((swift_name("currentPoints")));
@property void (^listener)(PSCPathEvent *) __attribute__((swift_name("listener")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RectF")))
@interface PSCRectF : PSCBase
- (instancetype)initWithLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom __attribute__((swift_name("init(left:top:right:bottom:)"))) __attribute__((objc_designated_initializer));
- (PSCRectF *)doCopyLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom __attribute__((swift_name("doCopy(left:top:right:bottom:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float bottom __attribute__((swift_name("bottom")));
@property (readonly) float left __attribute__((swift_name("left")));
@property (readonly) float right __attribute__((swift_name("right")));
@property (readonly) float top __attribute__((swift_name("top")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface PSCKotlinEnumCompanion : PSCBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) PSCKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface PSCKotlinArray<T> : PSCBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(PSCInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<PSCKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol PSCKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
