{
  description = "Riff - a desktop audio player (Electron)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    let
      mkRiff = pkgs:
        pkgs.buildNpmPackage rec {
          pname = "riff";
          version = "44.1.1";
          src = self;

          npmDepsHash = "sha256-eIBXB9u4dI8mcZ1pt4VRWT9U+EcXuGsVAeVwnGYGFmw=";

          ELECTRON_SKIP_BINARY_DOWNLOAD = "1";
          forceGitDeps = true;
          npmInstallFlags = [ "--omit=dev" ];

          buildPhase = "true";

          installPhase =
            let
              appDir = "$out/share/riff";
              runtimeDeps = pkgs.lib.makeBinPath [ pkgs.yt-dlp pkgs.ffmpeg ];
              desktop = pkgs.makeDesktopItem {
                name = "Riff";
                desktopName = "Riff";
                genericName = "Audio Player";
                exec = "riff";
                icon = "riff";
                categories = [ "AudioVideo" "Audio" "Player" ];
                startupWMClass = "riff";
              };
            in
            ''
              runHook preInstall

              mkdir -p ${appDir}
              for f in *; do cp -r "$f" ${appDir}/; done

              mkdir -p $out/bin
              makeWrapper ${pkgs.electron}/bin/electron $out/bin/riff \
                --add-flags ${appDir} \
                --prefix PATH : ${runtimeDeps}

              mkdir -p $out/share/applications
              cp ${desktop}/share/applications/*.desktop $out/share/applications/

              mkdir -p $out/share/icons/hicolor/256x256/apps
              cp ${appDir}/assets/icon.png $out/share/icons/hicolor/256x256/apps/riff.png

              runHook postInstall
            '';

          meta = with pkgs.lib; {
            description = "Riff - audio player made for humans";
            homepage = "https://github.com/rootscripts/riff";
            license = licenses.mit;
            mainProgram = "riff";
            platforms = platforms.linux ++ platforms.darwin;
          };
        };
    in
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        packages = {
          default = mkRiff pkgs;
          riff = mkRiff pkgs;
        };

        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            nodejs
            python3
            yt-dlp
            ffmpeg
          ];

          shellHook = ''
            echo "Riff dev shell. Install deps and run with:"
            echo "  npm install && npm start"
          '';
        };
      })
    // {
      overlays.default = final: prev: {
        riff = mkRiff final;
      };
    };
}
