
<p align="center">
    <img src="https://cdn.modrinth.com/data/cached_images/b04401997b0ef0d33053c6543c74534e35ade1fb.png" alt="Gleam Logo" />

![About](https://cdn.modrinth.com/data/cached_images/bbadb15a0bdc51b5808a4b995f7ac07d21e77aa5.png)

**Gleam** is an extremely powerful and very customizable colored lights mod that can render tens of thousands of colored lights at very high render distances with very little impact on FPS; it is by far the most capable colored lights mod to ever exist, intended for both casual players and mod developers looking to spice up the game's lighting engine.

Gleam is also fully compatible with [Sodium](https://modrinth.com/mod/sodium)!

![Screenshot](https://cdn.modrinth.com/data/cached_images/5618b2b35e5a40093c46cf2a5b8ea937a1d4eb54_0.webp)

![linebreak](https://cdn.modrinth.com/data/cached_images/2f3d8844608731843b20e16a4ab3f0135576ee26.png)

![Features](https://cdn.modrinth.com/data/cached_images/05275b69c1d7c0102191279cddf79f85f9af6b10.png)

Gleam provides a variety of features such as:

- Full RGB Colored Lights support with a vibrant pop to them!
- UV Blacklights that make saturated objects glow in the dark!
- Gleam is able to load as many as <u>16,384</u> lights at once!
- A very high render distance for rendering colored lights, within a 12x12 chunk radius.
- Blockstate specific colored lights.
- Incredibly Optimized *(see section below for more details)*
- Subtler approach to colored lights that isn't as overbearing as other mods in the genre
- Fully Resource Pack driven way of adding more lights with support for many vanilla and modded blocks, with an ever expanding list of support with the help of our community!
- And most importantly <u>**NO**</u> generative AI was used; because Gleam is a "shining ✨" example of how you dont need to use ai. You're welcome.

![linebreak](https://cdn.modrinth.com/data/cached_images/2f3d8844608731843b20e16a4ab3f0135576ee26.png)

![Optimized](https://cdn.modrinth.com/data/cached_images/959c7e49e3fa4c57266e90df046407dbfef36151.png)

As briefly mentioned earlier, Gleam is quite heavily optimized to ensure it can run as smoothly as possible and be able to handle tens of thousands of lights with very little impact on performance, it does this through a variety of tricks like Smart Light gathering, Chunk-Based GPU Buffering, LODs, Efficent usage of Resources and so on.

![Performance Example](https://cdn.modrinth.com/data/cached_images/bc763df642385bd8e10e40f875e0a156a3cbb1e9_0.webp)

Above is a screenshot taken while loading THOUSANDS of colored lights in a 192 blocks tall Nether Crimson Forest with **Sodium** and the game still runs at around a stable 60 FPS! results may vary from PC-to-PC but you can expect Gleam to run fairly smoothly assuming you have a modern computer with a decently good GPU and CPU.

![linebreak](https://cdn.modrinth.com/data/cached_images/2f3d8844608731843b20e16a4ab3f0135576ee26.png)

![Mod Support](https://cdn.modrinth.com/data/cached_images/9837e5f20f2e3df2424edd4a3955f22a8b67271f.png)

Gleam is made with mod compatibility in-mind with an ever expanding list of mods it supports! We have a contribution-driven basis for how we'll be adding compatibility; because of the easy to understand Resource Pack system. absolutely anyone can give any block a colored light and free to send over the JSONs to be added in future releases of Gleam.

If you are interested in contributing colored lights for modded blocks, you can Pull-Request to the Gleam Github Repository or manually send over the JSONs in our discord server.

Below are the current list of Mods supported by Gleam:
<details><summary>Currently Supported Mods</a></summary>

| Mod | Mod Version | Status | Contributors |
| -------- | -------- | -------- | -------- |
| [Jaden's Nether Expansion](https://modrinth.com/mod/jadens-nether-expansion) | 2.4.1 | Complete ✅| ThatJadenXgamer (ThatMaidenJaden) |

</details>


And a short Tutorial on how to add more colored lights with Resource Packs:
<details><summary>How to add Custom Colored Lights</a></summary>

In order to add more colored lights you need to make a JSON file within the following directory: `assets/<namespace>/gleam/light_providers/` the `namespace` can be anything of your choice; though I recommend using either `minecraft` or the namespace of whatever mod you intend on adding compat to.

## Overview
Here is the is the skeleton of a Gleam Light Provider JSON.
```json
{
  "emitters": [
    "modid:example_block", // You can provide a singleton or a list of blocks the light provider applies itself to.
    "modid:example_lamp[lit=true,facing=north]" // Supports blockstates using minecraft command syntax; it also allows multiple conditions.
  ],
  "light_properties": {
    "encoding": "hex", // you can specify the light for a block as either an "rgb" 0-1, "hsl" 0-255 or "hex" #RRGGBB.
    "hex": "#FF0000", // the light color.
    "radius": 8.0, // defines the radius that our colored light.
    "intensity": 1.0 // multiplier that defines the strength of the light, letting you brighten or dim its intensity.
  }
}
```

## Encodings
```json
		// The following only get parsed if encoding is set to "rgb"
		"encoding": "rgb",
		"red": 0.2,
		"green": 0.6,
		"blue": 1.0,
```

```json
		// the following only gets parsed if encoding is set to "hsl"
   		"encoding": "hsl",
		"hue": 149,
		"saturation": 255,
		"lightness": 153,
```

```json
		// the following only gets parsed if encoding is set to "hex"
   		"encoding": "hex",
		"hex": "#FF0000",
```


## UV Blacklights
In order to make a light source emit a UV Blacklight, you must set its color to pure black like so:

RGB - `red=0, green=0, blue=0`

HSL - `hue=0, saturation=0, lightness=0`

HEX - `#000000`

Blacklights only appear when the block is not exposed to any skylight and it is sufficently dark.

</details>

![linebreak](https://cdn.modrinth.com/data/cached_images/2f3d8844608731843b20e16a4ab3f0135576ee26.png)

![Roadmap](https://cdn.modrinth.com/data/cached_images/97a1783aeb35e52484bb7b0b6757e2214cec103c.png)

Gleam currently only supports Colored Lights but we do plan on implementing more features as time goes on, such as:

- **Bloom** - A popular visual effect most commonly seen in shaders that make bright objects bleed around it and look as if it's glowing.
- **Entity Colored Lights** - So far, entities are not affected by colored lights; but there are plans to make them similarly get tinted like the blocks do when near colored light sources.
- **Colored Dynamic Lights** - Standard run-of-the-mill dynamic lights where certain entities and held items would illuminate dark areas, but with support for colored lights.
- **Rim-light** - a popular suggestion by the community was to add outlines around blocks and entities in the world to replicate a similar effect to the offical promotional art.

![linebreak](https://cdn.modrinth.com/data/cached_images/2f3d8844608731843b20e16a4ab3f0135576ee26.png)

![Credits](https://cdn.modrinth.com/data/cached_images/8c551a5c87bb1d1d509fdb38e16c1d480ddd96df.png)

**[Shimmer](https://modrinth.com/mod/shimmer!)** - A small chunk of Gleam's colored lights code was derived from Shimmer's as a starting-off point, which I've since iterated on and changed quite drastically to fit my own needs for the project; though it is still important to remember that without Shimmer there would likely be no Gleam. And that is why I'd like to thank Shimmer and its Developers for their hard work!

![linebreak](https://cdn.modrinth.com/data/cached_images/2f3d8844608731843b20e16a4ab3f0135576ee26.png)

![FaQ](https://cdn.modrinth.com/data/cached_images/3c4a421125c41853eeab5326259bfbb4cd867ac1.png)

**Q: Is Gleam a client-side mod?**

A: Yes! don't need it on the server for it to work.

**Q: When will you update to [X] minecraft version?**

A: Gleam will be staying on 1.20.1 and 1.21.1 for the foreseeable future; there are no plans to move to 26.1+ and no explicit permission will be granted to port Gleam to any newer versions on my behalf.

**Q: Backports?**

A: I may backport Gleam to 1.20.1 sometime in the future, but for now I just intend on supporting 1.21.1; as for any other versions, I don't intend on backporting any further.

**Q: Fabric Port?**

A: Maybe.

**Q: Is this mod AI-generated?**

A: As stated earlier, <u>NO</u>. Everything in Gleam is <u>FULLY HUMAN-MADE</u>. Absolutely no generative AI content was used to make Gleam.

**Q: Does Gleam use Veil to add colored lights?**

A: No, gleam does its own thing to implement colored lights, but is also fully compatible and works with Veil assuming it's present!

![Gleam Logo...?](https://cdn.modrinth.com/data/cached_images/bbf9f8f7570af4562797baffab3e1d4286770933.png)

<a href="https://modrinth.com/mod/jadens-gleam">Gleam</a> © 2026 by <a href="https://github.com/ThatJadenXgamer">ThatJadenXgamer (ThatMaidenJaden)</a> is licensed under <a href="https://creativecommons.org/licenses/by-nc-nd/4.0/">CC BY-NC-ND 4.0</a>
<img src="https://mirrors.creativecommons.org/presskit/icons/cc.svg" alt="" width="20" height="20" style="margin-left: .2em;">
<img src="https://mirrors.creativecommons.org/presskit/icons/by.svg" alt="" width="20" height="20" style="margin-left: .2em;">
<img src="https://mirrors.creativecommons.org/presskit/icons/nc.svg" alt="" width="20" height="20" style="margin-left: .2em;">
<img src="https://mirrors.creativecommons.org/presskit/icons/nd.svg" alt="" width="20" height="20" style="margin-left: .2em;">