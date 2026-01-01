---
name: algorithmic-art
description: Create generative art using p5.js with seeded randomness. Use for creative coding, generative visuals, and algorithmic artwork.
---

# Algorithmic Art (p5.js)

## Two-Phase Process

| Phase | Output | Purpose |
|-------|--------|---------|
| 1. Philosophy | .md manifesto | Define algorithmic concept |
| 2. Implementation | .html artifact | Express through code |

## Philosophy Guidelines

Write 4-6 paragraphs describing:

| Element | Description |
|---------|-------------|
| Process | Computational processes, math relationships |
| Noise | Perlin noise, simplex, random patterns |
| Particles | Behaviors, fields, interactions |
| Time | Evolution, states, transitions |
| Emergence | Complexity from simple rules |

**Emphasize:** "meticulously crafted", "master-level implementation"

## Philosophy Examples

```
"Organic Turbulence"
Chaos constrained by natural law. Particles follow vector fields
derived from Perlin noise, creating flowing organic forms that
suggest wind, water, or plasma in constant flux.

"Quantum Harmonics"
Discrete entities with wave interference. Points oscillate with
frequencies that create interference patterns, standing waves,
and resonance zones across the canvas.

"Recursive Whispers"
Self-similarity across scales. Branching structures repeat at
decreasing scales, each iteration a whispered echo of the whole,
creating infinite depth in finite space.
```

## Implementation Template

```html
<!DOCTYPE html>
<html>
<head>
  <script src="https://cdn.jsdelivr.net/npm/p5@1.9.0/lib/p5.js"></script>
</head>
<body>
<script>
let seed = 12345;

function setup() {
  createCanvas(800, 800);
  randomSeed(seed);
  noiseSeed(seed);
}

function draw() {
  background(20);
  // Algorithm here
}

function keyPressed() {
  if (key === 'r') {
    seed = floor(random(99999));
    randomSeed(seed);
    noiseSeed(seed);
  }
  if (key === 's') {
    saveCanvas('art-' + seed, 'png');
  }
}
</script>
</body>
</html>
```

## Seeded Randomness

```javascript
// Same seed = same output (reproducible)
let seed = 42;
randomSeed(seed);
noiseSeed(seed);

// Seed controls
function nextSeed() { seed++; randomSeed(seed); noiseSeed(seed); }
function prevSeed() { seed--; randomSeed(seed); noiseSeed(seed); }
function jumpSeed(n) { seed = n; randomSeed(seed); noiseSeed(seed); }
```

## Parameter Design

Think: "What qualities can be adjusted?"

| Parameter Type | Examples |
|----------------|----------|
| Quantities | Particle count, iteration depth |
| Scales | Size, speed, frequency |
| Probabilities | Branch chance, color variation |
| Ratios | Aspect, proportion, balance |
| Angles | Direction, rotation speed |
| Thresholds | When behavior changes |

```javascript
const params = {
  particleCount: 500,
  noiseScale: 0.01,
  speed: 2,
  colorVariation: 0.2,
  branchProbability: 0.3
};
```

## Common Techniques

### Vector Fields
```javascript
let angle = noise(x * 0.01, y * 0.01) * TWO_PI * 2;
let v = p5.Vector.fromAngle(angle);
```

### Particle Systems
```javascript
class Particle {
  constructor() {
    this.pos = createVector(random(width), random(height));
    this.vel = createVector();
    this.acc = createVector();
  }

  follow(field) {
    let angle = field.lookup(this.pos);
    this.acc = p5.Vector.fromAngle(angle);
  }

  update() {
    this.vel.add(this.acc);
    this.vel.limit(4);
    this.pos.add(this.vel);
  }
}
```

### Noise Patterns
```javascript
// Organic flow
let n = noise(x * 0.01, y * 0.01, frameCount * 0.01);

// Fractal noise
let n = 0;
let amp = 1;
let freq = 0.01;
for (let i = 0; i < 4; i++) {
  n += noise(x * freq, y * freq) * amp;
  amp *= 0.5;
  freq *= 2;
}
```

### Color Harmony
```javascript
colorMode(HSB, 360, 100, 100);
let baseHue = random(360);
let c1 = color(baseHue, 70, 90);
let c2 = color((baseHue + 30) % 360, 50, 95);  // Analogous
let c3 = color((baseHue + 180) % 360, 60, 85); // Complementary
```

## Quality Standards

| Aspect | Standard |
|--------|----------|
| Balance | Complexity without noise |
| Color | Thoughtful palettes, not random |
| Composition | Visual hierarchy in randomness |
| Performance | Smooth 60fps |
| Reproducibility | Seeded, consistent |

## Avoid

| Anti-Pattern | Why |
|--------------|-----|
| Copying artists | Be original |
| Static output | Embrace generative nature |
| Random colors | Use harmonious palettes |
| Performance issues | Optimize for real-time |
| Unseeded random | Lose reproducibility |
