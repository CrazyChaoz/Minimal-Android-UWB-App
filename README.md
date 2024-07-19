# Minimal working UWB App

This is a minimal Android UWB App utilizing the currently available APIs (uwb:1.0.0-alpha08) for a very simple implementation.
This Project is written in plain Java and utilizes the user as out-of-band mechanism for configuration.

# Usage
1. Clone the repository
2. Build the project
   - Using Nix (nix build)
   - Using Android Studio or Gradle CLI
     - (OPTIONAL) You can delete the `gradle/verification-metadata.xml` file to disable dependency verification
3. Install the app on 2 UWB enabled Android phones
4. Open the app on both devices
5. One device has to be the Controller
6. Click on the "GET VALUES" button on both devices
7. Input the values from the other device into the text fields
8. Click on the "COMMUNICATE" button on both devices
