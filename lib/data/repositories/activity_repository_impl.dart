import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../domain/entities/activity.dart';
import '../../domain/entities/location.dart';
import '../../domain/entities/page.dart';
import '../../domain/repositories/activity_repository.dart';
import '../../core/utils/date_utils.dart' as app_date_utils;

/// Provider for the ActivityRepository implementation.
final activityRepositoryProvider =
    Provider<ActivityRepository>((ref) => ActivityRepositoryImpl());

/// Implementation of the ActivityRepository.
class ActivityRepositoryImpl extends ActivityRepository {
  static const platform = MethodChannel('com.beforbike.app/database');

  ActivityRepositoryImpl();

  @override
  Future<EntityPage<Activity>> getActivities({int pageNumber = 0}) async {
    // Get activities from local database
    final List<dynamic> activitiesData = await platform.invokeMethod('getAllActivities');

    List<Activity> activities = [];
    for (var activityMap in activitiesData) {
      final activity = await _mapDatabaseActivityToEntity(activityMap as Map<dynamic, dynamic>);
      activities.add(activity);
    }

    return EntityPage(list: activities, total: activities.length);
  }

  @override
  Future<EntityPage<Activity>> getUserActivities(String userId, {int pageNumber = 0}) async {
    return getActivities(pageNumber: pageNumber);
  }

  @override
  Future<Activity> getActivityById({required String id}) async {
    // Get activity from local database
    final List<dynamic> activitiesData = await platform.invokeMethod('getAllActivities');
    final activityMap = activitiesData.firstWhere(
      (activity) => activity['id'] == id,
      orElse: () => null,
    );

    if (activityMap != null) {
      return await _mapDatabaseActivityToEntity(activityMap as Map<dynamic, dynamic>);
    }

    throw Exception('Activity not found');
  }

  Future<Activity> _mapDatabaseActivityToEntity(Map<dynamic, dynamic> activityMap) async {
    // Get locations for this activity
    final locations = await _getLocationsForActivity(activityMap['id'] as String);

    return Activity(
      id: activityMap['id'] as String,
      startDatetime: app_date_utils.DateUtils.parseUniversalDate(activityMap['startDatetime']) ?? DateTime.now(),
      endDatetime: app_date_utils.DateUtils.parseUniversalDate(activityMap['endDatetime']) ?? DateTime.now(),
      distance: (activityMap['distance'] as num).toDouble(),
      speed: (activityMap['speed'] as num).toDouble(),
      cadence: (activityMap['cadence'] as num).toDouble(),
      calories: (activityMap['calories'] as num).toDouble(),
      power: (activityMap['power'] as num).toDouble(),
      altitude: (activityMap['altitude'] as num).toDouble(),
      time: (activityMap['time'] as num).toDouble(),
      locations: locations,
    );
  }

  Future<List<Location>> _getLocationsForActivity(String activityId) async {
    try {
      final List<dynamic> locationsData = await platform.invokeMethod('getActivityLocations', {'activityId': activityId});
      
      return locationsData.map((locationMap) {
        return Location(
          id: locationMap['id'] as String,
          datetime: app_date_utils.DateUtils.parseUniversalDate(locationMap['datetime']) ?? DateTime.now(),
          latitude: (locationMap['latitude'] as num).toDouble(),
          longitude: (locationMap['longitude'] as num).toDouble(),
        );
      }).toList();
    } catch (e) {
      return [];
    }
  }

  @override
  Future<String?> removeActivity({required String id}) async {
    await platform.invokeMethod('deleteActivity', {'activityId': id});
    return null; // Success
  }

}
