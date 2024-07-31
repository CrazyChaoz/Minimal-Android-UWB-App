{
  description = "A minimalistic Android UWB App";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
    android.url = "github:tadfisher/android-nixpkgs";
    nix-filter.url = github:numtide/nix-filter;
    gradle-dot-nix.url = "github:CrazyChaoz/gradle-dot-nix";
  };

  outputs = { self, nixpkgs, flake-utils, android, nix-filter, gradle-dot-nix}:
    let
        system = "x86_64-linux";
        inherit (nixpkgs) lib;
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
        android-sdk = android.sdk.${system} (sdkPkgs: with sdkPkgs; [
                    # Useful packages for building and testing.
                    build-tools-34-0-0
                    cmdline-tools-latest
                    platform-tools
                    platforms-android-34

                    # Other useful packages for a development environment.
                    # ndk-26-1-10909125
                    # skiaparser-3
                    # sources-android-34
                  ]
                  ++ lib.optionals (system == "aarch64-darwin") [
                    # system-images-android-34-google-apis-arm64-v8a
                    # system-images-android-34-google-apis-playstore-arm64-v8a
                  ]
                  ++ lib.optionals (system == "x86_64-darwin" || system == "x86_64-linux") [
                    # system-images-android-34-google-apis-x86-64
                    # system-images-android-34-google-apis-playstore-x86-64
                  ]);

        nix-filter-lib = import nix-filter;

        gradle-init-script = (import gradle-dot-nix {
                                               inherit pkgs;
                                               gradle-verification-metadata-file = ./gradle/verification-metadata.xml;
                                             }).gradle-init;
      in
      {
      packages.${system}.default =
      pkgs.stdenv.mkDerivation {
        name = "simple-uwb-app";
        src = nix-filter-lib {
          root = ./.;
          exclude = [
            (nix-filter-lib.matchExt "nix")
          ];
        };

        JDK_HOME = "${pkgs.jdk21.home}";
        ANDROID_HOME = "${android-sdk}/share/android-sdk";

        nativeBuildInputs = [
            android-sdk
            pkgs.gradle
            pkgs.jdk21
        ];
        buildPhase = ''
          gradle build --info -I ${gradle-init-script} --offline --full-stacktrace -Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/34.0.0/aapt2
        '';
        installPhase = ''
          mkdir -p $out
          cp -r ./app/build/outputs/apk/release/app-release-unsigned.apk $out
        '';
      };

      devShells.${system}.default = pkgs.mkShell {
          name = "android-dev";
          nativeBuildInputs = [
            android-sdk
            pkgs.gradle
            pkgs.jdk21
          ];
          shellHook = ''
            echo "Welcome to the Android development environment"
            echo "${android-sdk}"
          '';
        };
    };

}
