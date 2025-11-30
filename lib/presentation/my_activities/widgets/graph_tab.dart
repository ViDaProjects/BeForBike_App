import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';
import 'package:syncfusion_flutter_charts/charts.dart';

import '../../../domain/entities/activity.dart';
import '../../../domain/entities/location.dart';
import '../../common/core/utils/color_utils.dart';
import '../../common/core/utils/map_utils.dart';

class ChartData {
  final double x;
  final double y;
  ChartData(this.x, this.y);
}

class GraphData {
  final List<ChartData> data;
  final ChartData maxSpeedSpot;

  GraphData(this.data, this.maxSpeedSpot);
}

List<ChartData> smoothData(
  List<ChartData> inputSpots,
  double activityDistance,
) {
  final List<ChartData> smoothedSpots = [];

  final int windowSize = inputSpots.length > 10
      ? (inputSpots.length ~/ 3).clamp(5, 20)
      : 5;

  for (int i = 0; i < inputSpots.length; i++) {
    double sum = 0;
    int count = 0;

    for (int j = i - (windowSize ~/ 2); j <= i + (windowSize ~/ 2); j++) {
      if (j >= 0 && j < inputSpots.length) {
        sum += inputSpots[j].y;
        count++;
      }
    }

    final double smoothedValue = count != 0 ? sum / count : 0;
    smoothedSpots.add(ChartData(inputSpots[i].x, smoothedValue));
  }

  smoothedSpots.add(ChartData(activityDistance, inputSpots.last.y));

  return smoothedSpots;
}

List<ChartData> interpolatePoints(
  List<ChartData> points,
  int interpolationsPerSegment,
) {
  if (points.length < 2) return points;
  final List<ChartData> interpolated = [];
  for (int i = 0; i < points.length - 1; i++) {
    interpolated.add(points[i]);
    final double x1 = points[i].x;
    final double y1 = points[i].y;
    final double x2 = points[i + 1].x;
    final double y2 = points[i + 1].y;
    for (int j = 1; j <= interpolationsPerSegment; j++) {
      final double t = j / (interpolationsPerSegment + 1.0);
      final double x = x1 + t * (x2 - x1);
      final double y = y1 + t * (y2 - y1);
      interpolated.add(ChartData(x, y));
    }
  }
  interpolated.add(points.last);
  return interpolated;
}

GraphData computeGraphData(Activity activity) {
  final List<ChartData> spots = [];

  if (activity.locations.length > 1) {
    final List<Location> locations = activity.locations.toList();
    double totalDistance = 0;
    for (int i = 1; i < locations.length; i++) {
      final Location currentLocation = locations[i];
      final Location previousLocation = locations[i - 1];

      final double distance =
          MapUtils.getDistance(
            LatLng(previousLocation.latitude, previousLocation.longitude),
            LatLng(currentLocation.latitude, currentLocation.longitude),
          ) /
          1000;

      totalDistance += distance;

      final Duration timeDifference = currentLocation.datetime.difference(
        previousLocation.datetime,
      );

      double hoursDifference;

      timeDifference.inSeconds == 0
          ? hoursDifference = (timeDifference.inSeconds) / 3600
          : hoursDifference = (timeDifference.inMilliseconds) / 3600000;

      final double speed = hoursDifference > 0 ? distance / hoursDifference : 0;
      spots.add(ChartData(totalDistance, speed));
    }
  }

  final smoothedData = spots.isNotEmpty
      ? smoothData(spots, activity.distance)
      : spots;
  final interpolatedData = smoothedData.isNotEmpty
      ? interpolatePoints(smoothedData, 2)
      : smoothedData;
  final chartData = interpolatedData;
  final maxSpeedSpot = chartData.isNotEmpty
      ? chartData.reduce((a, b) => a.y > b.y ? a : b)
      : ChartData(0, 0);

  return GraphData(chartData, maxSpeedSpot);
}

/// The tab that displays a graph of speed of a specific activity.
class GraphTab extends StatelessWidget {
  final Activity activity;

  const GraphTab({super.key, required this.activity});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: FutureBuilder<GraphData>(
        future: compute(computeGraphData, activity),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError) {
            return Center(child: Text('Error: ${snapshot.error}'));
          } else if (!snapshot.hasData || snapshot.data!.data.isEmpty) {
            return Center(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  const Icon(Icons.info, size: 48),
                  const SizedBox(width: 8),
                  const Text(
                    'No Data',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                ],
              ),
            );
          } else {
            final graphData = snapshot.data!;
            final data = graphData.data;
            final maxSpeedSpot = graphData.maxSpeedSpot;

            return Column(
              children: [
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(10, 20, 10, 20),
                    child: SizedBox(
                      height: 200,
                      child: SfCartesianChart(
                        primaryXAxis: NumericAxis(
                          minimum: 0,
                          maximum: activity.distance,
                          interval: activity.distance > 2 ? 1 : 0.5,
                          title: AxisTitle(text: 'km'),
                          axisLabelFormatter: (AxisLabelRenderDetails details) {
                            return ChartAxisLabel(
                              details.value.toStringAsFixed(1),
                              const TextStyle(fontSize: 10),
                            );
                          },
                        ),
                        primaryYAxis: NumericAxis(
                          minimum: 0,
                          maximum: maxSpeedSpot.y * 1.25,
                          title: AxisTitle(text: 'km/h'),
                          axisLabelFormatter: (AxisLabelRenderDetails details) {
                            if (details.value == 0 ||
                                details.value == maxSpeedSpot.y * 1.25) {
                              return ChartAxisLabel('', const TextStyle());
                            }
                            return ChartAxisLabel(
                              details.value.toStringAsFixed(1),
                              const TextStyle(fontSize: 10),
                            );
                          },
                        ),
                        tooltipBehavior: TooltipBehavior(enable: true),
                        series: <CartesianSeries<ChartData, double>>[
                          SplineSeries<ChartData, double>(
                            dataSource: data,
                            xValueMapper: (ChartData data, _) => data.x,
                            yValueMapper: (ChartData data, _) => data.y,
                            splineType: SplineType.monotonic,
                            color: ColorUtils.main,
                            width: 2,
                            name: 'Speed',
                          ),
                          ScatterSeries<ChartData, double>(
                            dataSource: [maxSpeedSpot],
                            xValueMapper: (ChartData data, _) => data.x,
                            yValueMapper: (ChartData data, _) => data.y,
                            color: Colors.red,
                            markerSettings: MarkerSettings(
                              isVisible: true,
                              shape: DataMarkerType.circle,
                              width: 6,
                              height: 6,
                            ),
                            name: 'Max Speed',
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            );
          }
        },
      ),
    );
  }
}
