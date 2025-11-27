import 'package:equatable/equatable.dart';

/// Represents a location request object.
class LocationRequest extends Equatable {
  /// The latitude of the location.
  final double latitude;

  /// The longitude of the location.
  final double longitude;

  /// The datetime of the location.
  final DateTime datetime;

  /// Creates a new [LocationRequest] instance.
  const LocationRequest({
    required this.latitude,
    required this.longitude,
    required this.datetime,
  });

  /// Creates a [LocationRequest] from a map.
  factory LocationRequest.fromMap(Map<String, dynamic> map) {
    return LocationRequest(
      latitude: map['latitude'] as double,
      longitude: map['longitude'] as double,
      datetime: DateTime.fromMillisecondsSinceEpoch(map['datetime'] as int),
    );
  }

  /// Converts the [LocationRequest] to a map.
  Map<String, dynamic> toMap() {
    return {
      'latitude': latitude,
      'longitude': longitude,
      'datetime': datetime.millisecondsSinceEpoch,
    };
  }

  @override
  List<Object?> get props => [latitude, longitude, datetime];
}