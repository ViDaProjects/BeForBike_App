# BeForBike

A comprehensive cycling computer app that combines Flutter technology and native Android to provide an extensive cycling activity tracking experience.

## 📋 About the Project

This is a project developed as part of Integration Workshop 3, demonstrating the integration between Flutter (user interface) and native Android (background services and database). The app allows cyclists to track their activities in real-time, view detailed statistics, and analyze traveled routes.

## ✨ Features

### 🏃‍♂️ Real-Time Tracking
- **Real-time GPS**: Precise location tracking during cycling
- **Live metrics**: Speed, distance, cadence, and power
- **Intuitive interface**: Clean design that's easy to use during exercise

### 📊 Activity Analysis
- **Detailed statistics**: Duration, distance traveled, average/maximum speed
- **Interactive charts**: Visualization of speed, cadence, power, and altitude
- **Complete history**: Access to all previous activities

### 🗺️ Route Visualization
- **Interactive map**: Route visualization using OpenStreetMap
- **Elevation data**: Altitude analysis along the route

### 🔧 Technical Features
- **BLE Integration**: Native Android Bluetooth LE server implementation for sensor connectivity
- **Local database**: Efficient storage using SQLite on Android with statistics caching
- **Audio & Haptic Feedback**: Button interaction sounds and vibration patterns
- **Seed data**: Sample data for demonstration and testing
- **Cross-platform**: Android support (iOS disabled in this project)

## 🛠️ Technologies Used

### Frontend (Flutter)
- **Framework**: Flutter 3.38.3+
- **Language**: Dart 3.10.1+
- **State Management**: Riverpod + Hooks
- **UI Components**:
  - `fl_chart`: Interactive charts
  - `syncfusion_flutter_charts`: Advanced charting library
  - `flutter_map`: Maps with OpenStreetMap
  - `google_nav_bar`: Bottom navigation bar
- **Utilities**:
  - `geolocator`: Location services
  - `permission_handler`: Permission management
  - `shared_preferences`: Local storage
  - `audioplayers`: Audio playback
  - `vibration`: Device vibration
  - `wakelock_plus`: Screen wake lock

### Backend (Native Android)
- **SDK**: Gradle 9.2.1+
- **Java**: JDK 25+
- **Database**: SQLite with Room
- **BLE**: Native Android Bluetooth LE implementation
- **Services**: Background processing and statistics calculations

### Integration
- **MethodChannel**: Flutter ↔ Android communication for database operations and BLE
- **Platform Channels**: Data exchange between platforms
- **Async Operations**: Background processing for performance optimization

## 🚀 How to Run

### Prerequisites
- Flutter SDK 3.38.3 or higher
- Dart SDK 3.10.1 or higher
- Android Studio with Android SDK
- Android device or emulator
- JDK 25 or higher

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/ViDaProjects/BeForBike_App.git
   cd BeForBike_App
   ```

2. **Install dependencies**:
   ```bash
   flutter upgrade
   flutter pub upgrade
   flutter pub get
   ```

3. **Configure Android environment**:
   - Open the project in Android Studio
   - Sync Gradle
   - Configure a virtual device or connect a physical device

4. **Run the application in device**:
   ```bash
   flutter run --debug --hot
   ```

### 🧪 Running Tests

```bash
# Run all tests
flutter test

# Run tests with coverage
flutter test --coverage

# Run code analysis
flutter analyze
```

### �📱 Production Build

```bash
# Build for Android APK
flutter build apk --release

# Build for Android AAB (Google Play)
flutter build appbundle --release
```

## 📁 Project Structure

```
bicycle-computer-app/
├── android/                    # Native Android code
│   └── app/src/main/kotlin/com/beforbike/app/
│       ├── database/           # SQLite database and models
│       ├── MainActivity.kt     # Android entry point and MethodChannel
│       ├── BleServerService.kt # BLE GATT server implementation
│       └── GattProfile.kt      # BLE service definitions
├── lib/                        # Flutter code
│   ├── core/                   # Utilities and configurations
│   │   └── utils/              # Audio service, color utils
│   ├── data/                   # Data layer (repositories, APIs)
│   ├── domain/                 # Business rules (entities, repositories)
│   └── presentation/           # User interface
│       ├── common/             # Shared components and widgets
│       ├── home/               # Home screen and map view
│       ├── my_activities/      # Activity list and details
│       ├── settings/           # Settings screen
│       └── statistics/         # Statistics and charts
├── test/                       # Unit tests
└── pubspec.yaml                # Flutter dependencies
```

## 🔄 Architecture

The app follows a clean architecture with clear separation of responsibilities:

- **Presentation Layer**: Flutter widgets with Riverpod for state management
- **Domain Layer**: Business entities and repository contracts
- **Data Layer**: Repository implementations and Android communication
- **Platform Layer**: Native Android code for heavy services

### Data Flow
1. **Collection**: BLE sensors → Android SQLite database (background processing)
2. **Processing**: Statistics calculations with caching on Android
3. **Presentation**: Flutter reads cached data via MethodChannel
4. **Visualization**: Responsive interface with interactive charts and maps
