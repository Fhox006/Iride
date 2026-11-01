# Riff on Nix

The `flake.nix` exposes three things:

- `packages.riff` (also `packages.default`) — the installable app
- `overlays.default` — an overlay for nixpkgs
- `devShells.default` — a shell for hacking on Riff

## Install into your system config (Flakes)

Add the input:

```nix
{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    riff.url = "github:rootscripts/riff";
  };

  outputs = { self, nixpkgs, riff, ... }: {
    nixosConfigurations.myhost = nixpkgs.lib.nixosSystem {
      system = "x86_64-linux";
      modules = [
        ({ pkgs, ... }: {
          environment.systemPackages = [ riff.packages.x86_64-linux.default ];
        })
      ];
    };
  };
}
```

For home-manager, inside your home config:

```nix
{
  inputs.riff.url = "github:rootscripts/riff";

  outputs = { nixpkgs, home-manager, riff, ... }: {
    homeConfigurations.you = home-manager.lib.homeManagerConfiguration {
      pkgs = nixpkgs.legacyPackages.x86_64-linux;
      modules = [
        ({ pkgs, ... }: {
          home.packages = [ riff.packages.x86_64-linux.default ];
        })
      ];
    };
  };
}
```

## Install via an overlay

Useful when you want `pkgs.riff` available everywhere rather than referencing `riff.packages...` directly:

```nix
{
  inputs.riff.url = "github:rootscripts/riff";

  outputs = { nixpkgs, riff, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        overlays = [ riff.overlays.default ];
      };
    in {
      nixosConfigurations.myhost = pkgs.lib.nixosSystem {
        inherit system;
        modules = [{ environment.systemPackages = [ pkgs.riff ]; }];
      };
    };
}
```

## One-off / user installs (no config change)

```bash
# install into your profile
nix profile install github:rootscripts/riff

# or just run it once
nix run github:rootscripts/riff

# from a local checkout
nix profile install .
nix run .
```

After installing, `riff` is on your `PATH` and a `Riff` desktop entry + icon are registered.

## Development shell

```bash
git clone https://github.com/rootscripts/riff
cd riff
nix develop          # nodejs, python3, yt-dlp, ffmpeg
npm install
npm start
```

## Notes

- The package wraps the nixpkgs `electron` with the app directory and puts `yt-dlp` and `ffmpeg` on `PATH`, so Riff finds them instead of self-downloading.
- `npmDepsHash` in `flake.nix` must be updated whenever `package.json`/`package-lock.json` change. Rebuild with a wrong/placeholder hash and copy the reported `got:` value, or regenerate it with `nix-prefetch-npm-deps` / `prefetch-npm-deps`.
